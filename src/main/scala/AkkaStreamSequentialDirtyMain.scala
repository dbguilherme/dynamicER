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
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation

object AkkaStreamSequentialDirtyMain {

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
    val gtFile = Config.mainDir + Config.getsubDir() + Config.groundtruth + "IdDuplicates"

    if (Config.print) {
      println(s"Max memory: ${maxMemory} MB")
      println("File1\t:\t" + eFile1)
      println("gtFile\t:\t" + gtFile)
    }

    val eReader1 = new EntitySerializationReader(eFile1)
    val profiles1 = eReader1.getEntityProfiles.asScala.map((_,0)).toArray
    if (Config.print) System.out.println("Input Entity Profiles1\t:\t" + profiles1.size)

    val gtReader = new GtSerializationReader(gtFile)
    val dp = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null))
    if (Config.print) System.out.println("Existing Duplicates\t:\t" + dp.getDuplicates.size)

    // STEP 2. Initialize stream stages and flow
    implicit val system = ActorSystem("QuickStart") // TODO optional?

    // TODO in logic code Tokenizer and HSCompCleaner as objects? If objects problems in inner parallelization?
    val tokenizer: (((EntityProfile, Int), Long)) => (Int, TokenNGrams) = {
      case ((e,dId),id) => new Tokenizer().execute(id.toInt, dId, e)
    }

    val tokenBlocker = new TokenBlockerStage(Config.blocker, profiles1.size, 0, Config.cuttingRatio, Config.filteringRatio)
    val compCleaner = (lc: List[Comparison]) => ComparisonCleaning.apply(Config.ccMethod,Config.supervisedApproach,dp).execute(lc)
    val matcherFun = (c: Comparison) => {c.sim = c.e1Model.getSimilarity(c.e2Model); c}
    val matcher = (lc: List[Comparison]) => lc.map(matcherFun)

    val t1 = System.currentTimeMillis()
    val collector = new ProgressiveCollectorSink(t0, t1, dp, Config.print)

    val program =
      Source(profiles1).zipWithIndex
      .map(tokenizer)
      .via(tokenBlocker)
      .map (x => x match {
        case Comparisons(c) => new Comparisons(compCleaner(c))
        case Update(_,_) => x
      })
      .via(new StoreModelStage)
      .map(matcher)

    val done = program.runWith(collector)
    implicit val ec = system.dispatcher

    done.onComplete( result => {
      val t4 = System.currentTimeMillis()
      val counter = result.get
      system.terminate()
    })
  }
}


