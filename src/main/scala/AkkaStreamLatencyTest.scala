import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, _}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.util.ByteString
import com.parER.akka.streams.messages.{BlockTuple, Comparisons, Message, Update}
import com.parER.akka.streams.{ProcessingTokenBlockerStage, StoreModelStage}
import com.parER.core.blocking.{BlockGhostingFun, CompGenerationFun}
import com.parER.core.compcleaning.WNP2CompCleanerFun
import com.parER.core.{Config, TokenizerFun}
import com.parER.datastructure.Comparison
import org.scify.jedai.datamodel.EntityProfile
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.textmodels.TokenNGrams

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object AkkaStreamLatencyTest {

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

    val num = Config.pOption
    val time = Config.pOption2

    println(s"Source with rate ${num} x ${time} mills ;;;")

    val filename1 = Paths.get(s"AkkaStreamTestRate(${num}-${time})1.csv")
    val filename2 = Paths.get(s"AkkaStreamTestRate(${num}-${time})2.csv")

    val graph = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val bcast = b.add(Broadcast[((EntityProfile, Int), Long)](2))
      val merge     = b.add(Merge[((EntityProfile, Int), Long)](1))
      val sink = b.add(FileIO.toPath(filename1).async)
      bcast.out(0) ~> merge
      bcast.out(1) ~> Flow[((EntityProfile, Int), Long)].map(t => t match { case ((e,dId),id) =>
        ByteString(s"${id},${dId},${System.currentTimeMillis()}\n")
      }) ~> sink

      FlowShape(bcast.in, merge.out)
    })

    val program = Source.combine(
      Source(profiles1).zipWithIndex,
      Source(profiles2).zipWithIndex)(Merge(_))
      .throttle(num.toInt, FiniteDuration.apply(time, TimeUnit.MILLISECONDS))
      .via(graph)
      .map(tokenizer)
      .async
      .via(tokenBlocker)
      .async
      .via(partitionFlow)
      .async
      .via(new StoreModelStage(profiles1.size, profiles2.size))
      .async
      .buffer(128, OverflowStrategy.backpressure)
      .mapAsyncUnordered(nWorkers)(matcher)
      .buffer(128, OverflowStrategy.backpressure)
      .async
      .map(llc => {
        if (llc.size > 2) {
          val c1 = llc.head
          val c2 = llc.last
          if (c1.e1 == c2.e1) {
            ByteString(s"${c1.e1},${c1.e1Model.getDatasetId},${System.currentTimeMillis()},${llc.size},${c1.e1Model.getSignatures.size()}\n")
          } else {
            ByteString(s"${c1.e2},${c1.e2Model.getDatasetId},${System.currentTimeMillis()},${llc.size},${c1.e2Model.getSignatures.size()}\n")
          }
        } else
            ByteString("")
      })

    val t1 = System.currentTimeMillis()
    val done = program.runWith(FileIO.toPath(filename2))

    done.onComplete( result => {
      val OT = System.currentTimeMillis() - t1
      val ODT = System.currentTimeMillis() - t0
      println(s"OT: ${OT} ms")
      println(s"ODT: ${ODT} ms")
      system.terminate()
    })
  }
}