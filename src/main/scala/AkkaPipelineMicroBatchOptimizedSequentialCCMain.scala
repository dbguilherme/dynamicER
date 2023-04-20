import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, _}
import akka.stream.{FlowShape, OverflowStrategy}
import com.parER.akka.streams.messages._
import com.parER.akka.streams.{ProcessingSeqTokenBlockerStage, ProgressiveCollectorSink, StoreSeqModelStage}
import com.parER.core.blocking.{BlockGhostingFun, CompGenerationFun}
import com.parER.core.compcleaning.WNP2CompCleanerFun
import com.parER.core.{Config, TokenizerFun}
import com.parER.datastructure.Comparison
import com.parER.utils.CsvWriter
import org.scify.jedai.datamodel.EntityProfile
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation

import scala.concurrent.duration.FiniteDuration

object AkkaPipelineMicroBatchOptimizedSequentialCCMain {

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

    val groupNum  = Config.pOption
    val millNum = Config.pOption2

    println(s"nBlockers: ${nBlockers}")
    println(s"nCleaners: ${nCleaners}")
    println(s"nWorkers: ${nWorkers}")
    println(s"groupNum: ${groupNum}")
    println(s"millNum: ${millNum}")

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
    //println(ec.toString)

    val tokenizer = (lc: Seq[((EntityProfile, Int), Long)]) =>  {
      lc.map( x => x match {case ((e,dId),id) => TokenizerFun.execute(id.toInt, dId, e)} )
    }

    val tokenBlocker  =   new ProcessingSeqTokenBlockerStage(Config.blocker, profiles1.size, profiles2.size, Config.cuttingRatio, Config.filteringRatio)
    val matcherFun    =   (c: Comparison) => { c.sim = c.e1Model.getSimilarity(c.e2Model); c }
    val matcher       =   (lc: List[Comparison]) => lc.map(matcherFun)

    val partitionFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val ff        = Config.filteringRatio
      val merge     = b.add(MergePreferred[Message](1))
      val partition = b.add(Partition[Message](2, m => {
        m match {
          case UpdateSeq(_) => 0
          case BlockTupleSeq(_) => 1
        }
      }))

      val bGhost = Flow[Message].map( x => x match { case BlockTupleSeq(s) => {
        BlockTupleSeq(s.map(e => {
          val t = BlockGhostingFun.process(e.id, e.model, e.blocks, ff)
          BlockTuple(t._1, t._2, t._3)
        }))
      }})

      val cGener = Flow[BlockTupleSeq].map( s =>
        ComparisonsSeq(s.blockTupleSeq.map(e => Comparisons(CompGenerationFun.generateComparisons(e.id, e.model, e.blocks))))
      )

      val compCl = Flow[ComparisonsSeq].map( s =>
        ComparisonsSeq(s.comparisonSeq.map(e => Comparisons(WNP2CompCleanerFun.execute(e.comparisons))))
      )

      partition.out(0) ~> merge.in(0)
      partition.out(1) ~> bGhost ~>
        cGener ~>
        compCl.buffer(128, OverflowStrategy.backpressure) ~> merge.in(1)
      FlowShape(partition.in, merge.out)
    })

    val t1 = System.currentTimeMillis()
    val collector = new ProgressiveCollectorSink(t0, t1, dp, Config.print)

    val program = Source.combine(
      Source(profiles1).zipWithIndex,
      Source(profiles2).zipWithIndex)(Merge(_))
      .groupedWithin(groupNum.toInt, FiniteDuration.apply(millNum, TimeUnit.MILLISECONDS))
      .map(tokenizer)
      .via(tokenBlocker)
      .via(partitionFlow)
      .via(new StoreSeqModelStage(profiles1.size, profiles2.size))
      .mapConcat(lc => {
        if (lc.size > 1000*nWorkers)
          lc.grouped(lc.size/nWorkers).toSeq
        else
          lc.grouped(lc.size).toSeq
      })
      .buffer(128, OverflowStrategy.backpressure)
      .map(matcher)
      .buffer(128, OverflowStrategy.backpressure)

    val done = program.runWith(collector)

    done.onComplete( result => {
      val OT = System.currentTimeMillis() - t1
      val ODT = System.currentTimeMillis() - t0

      if (Config.output) {

        val csv = new CsvWriter("name,n,nb,nc,nw,OT,ODT")
        val name = s"MicroBatchPipelineSequential-${groupNum}-${millNum}"

        // Put dimension of thread pool
        val n = (nBlockers+nCleaners+nWorkers).toString
        val nb = nBlockers.toString
        val nc = nCleaners.toString
        val nw = nWorkers.toString
        val line = List[String](name, n, nb, nc, nw, OT.toString, ODT.toString)
        csv.newLine(line)
        csv.writeFile(Config.file, Config.append)
      }

      system.terminate()
    })
  }
}