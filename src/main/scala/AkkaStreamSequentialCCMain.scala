import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.parER.akka.streams.messages.{Comparisons, Update}
import com.parER.akka.streams.{ProgressiveCollectorSink, StoreModelStage, TokenBlockerStage}
import com.parER.core.compcleaning.ComparisonCleaning
import com.parER.core.{Config, Tokenizer}
import com.parER.datastructure.Comparison
import org.scify.jedai.datamodel.EntityProfile
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation

object AkkaStreamSequentialCCMain {

  def main(args: Array[String]): Unit = {
    import scala.jdk.CollectionConverters._

    val maxMemory = Runtime.getRuntime().maxMemory() / math.pow(10, 6)

    // Argument parsing
    Config.commandLine(args)
    val priority = Config.priority
    val dataset1 = Config.dataset1
    val dataset2 = Config.dataset2
    val threshold = Config.threshold

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
    implicit val system = ActorSystem("QuickStart") // TODO optional?
    implicit val ec = system.dispatcher

    val tokenizer: (((EntityProfile, Int), Long)) => (Int, TokenNGrams) = {
      case ((e,dId),id) => new Tokenizer().execute(id.toInt, dId, e)
    }

    val tokenBlocker = new TokenBlockerStage(Config.blocker, profiles1.size, profiles2.size, Config.cuttingRatio, Config.filteringRatio)
    val compCleanerFun = (lc: List[Comparison]) => ComparisonCleaning.apply(Config.ccMethod).execute(lc)
    val matcherFun = (c: Comparison) => {c.sim = c.e1Model.getSimilarity(c.e2Model); c}
    val matcher = (lc: List[Comparison]) => lc.map(matcherFun)

    val t1 = System.currentTimeMillis()
    val collector = new ProgressiveCollectorSink(t0, t1, dp, true)

    val program = Source.combine(
      Source(profiles1).zipWithIndex,
      Source(profiles2).zipWithIndex)(Merge(_))
      .map(tokenizer)
      .via(tokenBlocker)
      .map (x => x match {
        case Comparisons(c) => Comparisons(compCleanerFun(c))
        case Update(_,_) => x
      })
      .via(new StoreModelStage)
      .map(matcher)

    val done = program.runWith(collector)

    done.onComplete( result => {
      val delta = System.currentTimeMillis() - t1
      println(s"1: ${delta} ms")
      system.terminate()
    })
  }
}


