import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, _}
import akka.stream.{FlowShape, OverflowStrategy}
import com.parER.akka.streams.messages.{BlockTuple, Comparisons, Message, Update}
import com.parER.akka.streams.{ProcessingTokenBlockerStage, ProgressiveCollectorSink, StoreModelStage, StoreSeqModelStage}
import com.parER.core.blocking.{BlockGhosting, BlockGhostingFun, CompGeneration, CompGenerationFun}
import com.parER.core.collecting.ProgressiveCollector
import com.parER.core.compcleaning.{ComparisonCleaning, WNP2CompCleanerFun}
import com.parER.core.{Config, Tokenizer, TokenizerFun}
import com.parER.datastructure.Comparison
import com.parER.utils.CsvWriter
import org.scify.jedai.datamodel.EntityProfile
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation

import scala.concurrent.Future

object AkkaPipelineCCMain {

  def main(args: Array[String]): Unit = {
    import scala.jdk.CollectionConverters._

    val maxMemory = Runtime.getRuntime().maxMemory() / math.pow(10, 6)

    // Argument parsing
    Config.commandLine(args)
    val priority = Config.priority
    val dataset1 = Config.dataset1
    val dataset2 = Config.dataset2
    val threshold = Config.threshold

    val nBlockers = Config.blockers
    val nWorkers = Config.workers
    val nCleaners = Config.cleaners

    println(s"nBlockers: ${nBlockers}")
    println(s"nCleaners: ${nCleaners}")
    println(s"nWorkers: ${nWorkers}")

    // STEP 1. Initialization and read dataset - gt file
    val t0 = System.currentTimeMillis()
    val eFile1  = Config.mainDir + Config.getsubDir() + Config.dataset1 + "Profiles"
    val eFile2  = Config.mainDir + Config.getsubDir() + Config.dataset2 + "Profiles"
    val gtFile = Config.mainDir + Config.getsubDir() + Config.groundtruth + "IdDuplicates"

    if (Config.print) {
      println(s"Max memory: ${maxMemory} MB")
      println("File1\t:\t" + eFile1)
      println("File2\t:\t" + eFile2)
      println("gtFile\t:\t" + gtFile)
    }

    val eReader1 = new EntitySerializationReader(eFile1)
    val profiles1 = eReader1.getEntityProfiles.asScala.map((_,0)).toArray
    if (Config.print) System.out.println("Input Entity Profiles1\t:\t" + profiles1.size)

    val eReader2 = new EntitySerializationReader(eFile2)
    val profiles2 = eReader2.getEntityProfiles.asScala.map((_,1)).toArray
    if (Config.print) System.out.println("Input Entity Profiles2\t:\t" + profiles2.size)

    val gtReader = new GtSerializationReader(gtFile)
    val dp = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null))
    if (Config.print) System.out.println("Existing Duplicates\t:\t" + dp.getDuplicates.size)

    // STEP 2. Initialize stream stages and flow
    implicit val system = ActorSystem("StreamingER")
    implicit val ec = system.dispatcher

    val tokenizer: (((EntityProfile, Int), Long)) => (Int, TokenNGrams) = {
      case ((e,dId),id) => TokenizerFun.execute(id.toInt, dId, e)
    }

    val tokenBlocker  =   new ProcessingTokenBlockerStage(Config.blocker, profiles1.size, profiles2.size, Config.cuttingRatio, Config.filteringRatio)
    val matcherFun    =   (c: Comparison) => { c.sim = c.e1Model.getSimilarity(c.e2Model); c }
    def matcher(lc: List[Comparison]) = Future {
      lc.map(matcherFun)
    }

    val partitionFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val ff        = Config.filteringRatio
      val merge     = b.add(Merge[Message](2))
      val partition = b.add(Partition[Message](2, m => {
        m match {
          case Update(_,_) => 0
          case BlockTuple(_,_,_) => 1
        }
      }))

      val bGhost = Flow[Message].map( x => x match { case BlockTuple(id, model, blocks) =>
        val t = BlockGhostingFun.process(id, model, blocks, ff)
        BlockTuple(t._1, t._2, t._3)
      })
      val cGener = Flow[BlockTuple].mapAsyncUnordered(nBlockers)( x => Future.apply { Comparisons( CompGenerationFun.generateComparisons(x.id, x.model, x.blocks) ) })
      val compCl = Flow[Comparisons].mapAsyncUnordered(nCleaners)( x => Future.apply { Comparisons( WNP2CompCleanerFun.execute(x.comparisons) ) } )

      partition.out(0) ~> merge.in(0)
      partition.out(1) ~>
        bGhost.async ~>
        cGener.async ~>
        compCl.buffer(128, OverflowStrategy.backpressure) ~> merge.in(1)
      FlowShape(partition.in, merge.out)
    })

    val program = Source.combine(
      Source(profiles1).zipWithIndex,
      Source(profiles2).zipWithIndex)(Merge(_))
      .map(tokenizer)
      .async
      .via(tokenBlocker)
      .async
      .via(partitionFlow)
      .async
      .via(new StoreModelStage(profiles1.size, profiles2.size))
      .mapConcat(lc => {
        if (lc.size > 1000*nWorkers)
          lc.grouped(lc.size/nWorkers).toSeq
        else
          lc.grouped(lc.size).toSeq
      })
      .async
      .buffer(128, OverflowStrategy.backpressure)
      .mapAsyncUnordered(nWorkers)(matcher)
      .buffer(128, OverflowStrategy.backpressure)
      .async

    val t1 = System.currentTimeMillis()
    val proColl = new ProgressiveCollector(t0, t1, dp, Config.print)
    val done = program.runForeach(proColl.execute)

    done.onComplete( result => {
      val OT = System.currentTimeMillis() - t1
      val ODT = System.currentTimeMillis() - t0
      proColl.printLast()

      if (Config.output) {

        val csv = new CsvWriter("name,OT,ODT,N,Ncg,Ncc,Nw")
        val name = s"ParPipeline+Split"

        // Put dimension of thread pool
        val n = (5+nBlockers+nCleaners+nWorkers).toString
        val ncg = nBlockers.toString
        val ncc = nCleaners.toString
        val nw = nWorkers.toString
        val line = List[String](name, OT.toString, ODT.toString, n, ncg, ncc, nw)
        csv.newLine(line)
        csv.writeFile(Config.file, Config.append)
      }

      system.terminate()
    })
  }
}