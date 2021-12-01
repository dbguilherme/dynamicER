import com.parER.core.blocking.{BlockGhosting, Blocking, CompGeneration, StoreModel}
import com.parER.core.collecting.ProgressiveCollector
import com.parER.core.compcleaning.ComparisonCleaning
import com.parER.core.matching.Matcher
import com.parER.core.{Config, Tokenizer}
import com.parER.datastructure.Comparison
import com.parER.utils.CsvWriter
import org.scify.jedai.datamodel.EntityProfile
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation

object SequentialFixedIncrementalCCMain extends App {
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

    def processEntity(i:Int, dIdx:Int, profile:EntityProfile)={
        var t = System.currentTimeMillis()
        var comps = List[Comparison]()
        val (id1, obj1) = tokenizer.execute(i, dIdx, profile)
        tTokenizer += (System.currentTimeMillis() - t)

        t = System.currentTimeMillis()
        val tuple = tokenBlocker.process(id1, obj1)
        tBlocker += (System.currentTimeMillis() - t)
        //cBlocker += comps1.size

        t = System.currentTimeMillis()
        val bIds = blockGhoster.process(tuple._1, tuple._2, tuple._3)
        tBlockGhosting += (System.currentTimeMillis() - t)

        t = System.currentTimeMillis()
        comps = compGeneration.generateComparisons(bIds._1, bIds._2, bIds._3)
        tCompGeneration += (System.currentTimeMillis() - t)
        cBlocker += comps.size

        t = System.currentTimeMillis()
        comps = compCleaner.execute(comps)
        tCompCleaner += (System.currentTimeMillis() - t)
        cCompCleaner += comps.size

        t = System.currentTimeMillis()
        storeModel.solveUpdate(id1, obj1)
        comps = storeModel.solveComparisons(comps)
        tStoreModel += (System.currentTimeMillis() - t)
        comps
    }


    val N = Config.batches // number of batches
    val chunkSize1 = (profiles1.size.toDouble/N.toDouble).ceil.toInt
    val groupedProfiles1 = profiles1.grouped(chunkSize1).toList

    val chunkSize2 = (profiles2.size.toDouble/N.toDouble).ceil.toInt
    val groupedProfiles2 = profiles2.grouped(chunkSize2).toList

    var aggTime = 0L
    for ( inc <- 1 to N) {

        println(s"====== increment ${inc} =======")

        var tStart =  System.currentTimeMillis()

        // Time counters are resetted for processEntity function
        // Comparison counters are not resetted, they will aggregate values
        tTokenizer = 0L
        tBlocker = 0L
        tCompCleaner = 0L
        tMatcher = 0L
        tCollector = 0L
        tStoreModel = 0L
        tBlockGhosting = 0L
        tCompGeneration = 0L

        val p1 = groupedProfiles1(inc-1)
        val p2 = groupedProfiles2(inc-1)

        val t1 = System.currentTimeMillis()

        // STEP 3. iterative computation -> (Assume CCER)
        var i = 0
        val n = math.max(p1.length, p2.length)
        while (i < n) {
            var t = 0L
            var comps1 = List[Comparison]()
            var comps2 = List[Comparison]()

            if (p1.size > i) {
                val idx = (inc-1) * chunkSize1 + i
                comps1 = processEntity(idx, 0, p1(i))
            }

            if (p2.size > i) {
                val idx = (inc-1) * chunkSize2 + i
                comps2 = processEntity(idx, 1, p2(i))
            }

            if (compMatcher != null) {
                t = System.currentTimeMillis()
                comps1 = compMatcher.execute(comps1 ++ comps2)
                tMatcher += (System.currentTimeMillis() - t)

                t = System.currentTimeMillis()
                proCollector.execute(comps1)
                tCollector += (System.currentTimeMillis() - t)
            }

            i += 1
        }

        var tEnd = System.currentTimeMillis() - tStart
        aggTime+=tEnd;

        if (Config.output) {
            val csv = new CsvWriter("method, increment, time, aggregate-time, comparisons, name, CoCl, ro, ff, PC, PQ, BB(T), CC(T), MA(T), O(T), OD(T), BB(C), CC(C)")

            val CoCl = Config.ccMethod
            val ro = Config.cuttingRatio.toString
            val ff = Config.filteringRatio.toString
            val p = if (smBool) "sm-" else ""
            val name = p+CoCl + "-" + ro + "-" + ff

            val PC = proCollector.getPC.toString
            val PQ = proCollector.getPQ.toString
            val BBT = (tTokenizer + tBlocker + tBlockGhosting + tCompGeneration + tStoreModel).toString
            val CCT = tCompCleaner.toString
            val MAT = tMatcher.toString
            val OT = (tTokenizer + tBlocker + tBlockGhosting + tCompGeneration + tStoreModel + tCompCleaner + tMatcher + tCollector).toString
            val ODT = (System.currentTimeMillis() - t0).toString
            val BBC = cBlocker.toString
            val CCC = cCompCleaner.toString

            val line = List[String]("i-wnp", inc.toString, tEnd.toString, aggTime.toString, CCC, name, CoCl, ro, ff, PC, PQ, BBT, CCT, MAT, OT, ODT, BBC, CCC)
            println("method, increment, time, aggregate-time, comparisons, name, CoCl, ro, ff, PC, PQ, BB(T), CC(T), MA(T), O(T), OD(T), BB(C), CC(C)")
            println(line)
            //val line = List[String](name, CoCl, ro, ff, PC, PQ, BBT, CCT, MAT, OT, ODT, BBC, CCC)
            csv.newLine(line)
            csv.writeFile(Config.file, Config.append || inc != 1)
        }

    }

//    proCollector.printLast()
//
//    println("\nTime measurements: ")
//    println("tTokenizer = " + tTokenizer + " ms")
//    println("tBlocker = " + tBlocker + " ms")
//    println("tBlockGhosting = " + tBlockGhosting + " ms")
//    println("tCompGeneration = " + tCompGeneration + " ms")
//    println("tCompCleaner = " + tCompCleaner + " ms")
//    println("tMatcher = " + tMatcher + " ms")
//    println("tCollector = " + tCollector + " ms")
//    println("tStoreModel = " + tStoreModel + " ms")
//    println("PC = " + proCollector.getPC())
//
//    println("Comparisons after blocking: " + cBlocker)
//    println("Comparisons after comparison cleaning: " + cCompCleaner)

//    if (Config.output) {
//        val csv = new CsvWriter("name,dr,bb+bp,bg,cg,cc,lm,co,cl,sum,RT,PC")
//        val name = dataset1+Config.cuttingRatio
//        val dr = tTokenizer
//        val bb_bp = tBlocker
//        val bg = tBlockGhosting
//        val cg = tCompGeneration
//        val cc = tCompCleaner
//        val lm = tStoreModel
//        val co = tMatcher
//        val cl = tCollector
//        val sum = dr+bb_bp+bg+cg+cc+lm+co+cl
//        val RT = (System.currentTimeMillis() - t1).toString
//        val PC = proCollector.getPC.toString
//
//        val line = List[String](name,dr.toString,bb_bp.toString,bg.toString,cg.toString,cc.toString,lm.toString,co.toString,cl.toString,sum.toString,RT.toString,PC)
//        println(name)
//        println(line)
//        csv.newLine(line)
//        csv.writeFile(Config.file, Config.append)
//    }




}