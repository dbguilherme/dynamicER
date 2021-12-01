
import com.parER.core.blocking.{BlockGhosting, Blocking, CompGeneration, StoreModel}
import com.parER.core.collecting.ProgressiveCollector
import com.parER.core.compcleaning.ComparisonCleaning
import com.parER.core.matching.Matcher
import com.parER.core.{Config, Tokenizer}
import com.parER.datastructure.Comparison
import com.parER.utils.CsvWriter
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation

object SequentialLatencyCCMain extends App {
    import scala.jdk.CollectionConverters._

    val maxMemory = Runtime.getRuntime().maxMemory() / math.pow(10, 6)

    // Time variables
    var tTokenizer = 0L
    var tBlocker = 0L
    var tCompCleaner = 0L
    var tMatcher = 0L
    var tCollector = 0L
    var tStoreModel = 0L
    var tBlockGhosting = 0L
    var tCompGeneration = 0L

    // Comparison counters
    var cBlocker = 0L
    var cCompCleaner = 0L

    // Argument parsing
    Config.commandLine(args)
    val priority = Config.priority
    val dataset1 = Config.dataset1
    val dataset2 = Config.dataset2
    val threshold = Config.threshold
    val smBool = Config.storeModel

    println(s"Store model ${smBool}")

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
    val profiles1 = eReader1.getEntityProfiles.asScala.toArray
    if (Config.print) System.out.println("Input Entity Profiles1\t:\t" + profiles1.size)

    val eReader2 = new EntitySerializationReader(eFile2)
    val profiles2 = eReader2.getEntityProfiles.asScala.toArray
    if (Config.print) System.out.println("Input Entity Profiles2\t:\t" + profiles2.size)

    val gtReader = new GtSerializationReader(gtFile)
    val dp = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null))
    if (Config.print) System.out.println("Existing Duplicates\t:\t" + dp.getDuplicates.size)

    // STEP 2. functional stages
    val tokenizer = new Tokenizer
    val tokenBlocker = Blocking.apply(Config.blocker, profiles1.size, profiles2.size, Config.cuttingRatio, Config.filteringRatio)
    val compCleaner = ComparisonCleaning.apply(Config.ccMethod)
    val compMatcher = Matcher.apply(Config.matcher)
    val proCollector = new ProgressiveCollector(t0, System.currentTimeMillis(), dp, Config.print)

    val blockGhoster = new BlockGhosting(Config.filteringRatio)
    val compGeneration = new CompGeneration

    val storeModel = new StoreModel(profiles1.size, profiles2.size)
    tokenBlocker.setModelStoring(!smBool)

    val t1 = System.currentTimeMillis()

    val csv = new CsvWriter("id,did,latency,comparisons")

    // STEP 3. iterative computation -> (Assume CCER)
    var i = 0
    val n = math.max(profiles1.length, profiles2.length)
    while (i < n) {
        var t = 0L
        var comps1 = List[Comparison]()
        var comps2 = List[Comparison]()

        if (profiles1.size > i) {
            val t0 = System.currentTimeMillis()
            t = System.currentTimeMillis()
            val (id1, obj1) = tokenizer.execute(i, 0, profiles1(i))
            tTokenizer += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            val tuple = tokenBlocker.process(id1, obj1)
            tBlocker += (System.currentTimeMillis() - t)
            //cBlocker += comps1.size

            t = System.currentTimeMillis()
            val bIds = blockGhoster.process(tuple._1, tuple._2, tuple._3)
            tBlockGhosting += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            comps1 = compGeneration.generateComparisons(bIds._1, bIds._2, bIds._3)
            tCompGeneration += (System.currentTimeMillis() - t)
            cBlocker += comps1.size

            t = System.currentTimeMillis()
            comps1 = compCleaner.execute(comps1)
            tCompCleaner += (System.currentTimeMillis() - t)
            cCompCleaner += comps1.size

            t = System.currentTimeMillis()
            storeModel.solveUpdate(id1, obj1)
            comps1 = storeModel.solveComparisons(comps1)
            tStoreModel += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            comps1 = compMatcher.execute(comps1)
            tMatcher += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            proCollector.execute(comps1)
            tCollector += (System.currentTimeMillis() - t)

            val t1 = System.currentTimeMillis() - t0
            val line = List[String](i.toString,0.toString,t1.toString,comps1.size.toString)
            csv.newLine(line)
        }

        if (profiles2.size > i) {
            val t0 = System.currentTimeMillis()
            t = System.currentTimeMillis()
            val (id2, obj2) = tokenizer.execute(i, 1, profiles2(i))
            tTokenizer += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            val tuple = tokenBlocker.process(id2, obj2)
            tBlocker += (System.currentTimeMillis() - t)
            //cBlocker += comps1.size

            t = System.currentTimeMillis()
            val bIds = blockGhoster.process(tuple._1, tuple._2, tuple._3)
            tBlockGhosting += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            comps2 = compGeneration.generateComparisons(bIds._1, bIds._2, bIds._3)
            tCompGeneration += (System.currentTimeMillis() - t)
            cBlocker += comps2.size

            t = System.currentTimeMillis()
            comps2 = compCleaner.execute(comps2)
            tCompCleaner += (System.currentTimeMillis() - t)
            cCompCleaner += comps1.size

            t = System.currentTimeMillis()
            storeModel.solveUpdate(id2, obj2)
            comps2 = storeModel.solveComparisons(comps2)
            tStoreModel += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            comps2 = compMatcher.execute(comps2)
            tMatcher += (System.currentTimeMillis() - t)

            t = System.currentTimeMillis()
            proCollector.execute(comps2)
            tCollector += (System.currentTimeMillis() - t)

            val t1 = System.currentTimeMillis() - t0
            val line = List[String](i.toString,1.toString,t1.toString,comps2.size.toString)
            csv.newLine(line)
        }

        i += 1
    }

    proCollector.printLast()

    println("\nTime measurements: ")
    println("tTokenizer = " + tTokenizer + " ms")
    println("tBlocker = " + tBlocker + " ms")
    println("tBlockGhosting = " + tBlockGhosting + " ms")
    println("tCompGeneration = " + tCompGeneration + " ms")
    println("tCompCleaner = " + tCompCleaner + " ms")
    println("tMatcher = " + tMatcher + " ms")
    println("tCollector = " + tCollector + " ms")
    println("tStoreModel = " + tStoreModel + " ms")
    println("PC = " + proCollector.getPC())

    println("Comparisons after blocking: " + cBlocker)
    println("Comparisons after comparison cleaning: " + cCompCleaner)

    csv.writeFile(Config.file, Config.append)
}