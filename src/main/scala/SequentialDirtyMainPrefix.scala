
import com.parER.core.blocking.{Blocking, CompGeneration, StoreModel}
import com.parER.core.collecting.ProgressiveCollector
import com.parER.core.compcleaning.{ComparisonCleaning, PositionalFilter}
import com.parER.core.matching.JSMatcher
import com.parER.core.{Config, Tokenizer}
import com.parER.datastructure.Comparison
import com.parER.utils.CsvWriter
import org.scify.jedai.datareader.entityreader.EntitySerializationReader
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation

object SequentialDirtyMainPrefix extends App {
    import scala.jdk.CollectionConverters._

    val maxMemory = Runtime.getRuntime().maxMemory()  / math.pow(10, 6)

    val beforeUsedMem = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    /**  Time variables **/
    var tTokenizer = 0.0D //0L para long e 0.0D para double
    var tBlocker = 0.0D
    var tCompCleaner = 0.0D
    var tMatcher = 0.0D
    var tCollector = 0.0D
    var tStoreModel = 0.0D
    var tBlockGhosting = 0.0D
    var tCompGeneration = 0.0D

    /** Comparison counters **/
    var cBlocker = 0L
    var cCompCleaner = 0L

    /** Argument parsing **/
    Config.commandLine(args)
    val priority = Config.priority
    val dataset1 = Config.dataset1
    val dataset2 = Config.dataset2
    val threshold = Config.threshold
    val smBool = Config.storeModel

    //println(s"Store model ${smBool}")

    /** STEP 1. Initialization and read dataset - gt file **/
    val t0 = System.currentTimeMillis()
    val eFile1  = Config.mainDir + Config.getsubDir() + Config.dataset1 + "Profiles"
    val gtFile = Config.mainDir + Config.getsubDir() + Config.groundtruth + "IdDuplicates"

    if (Config.print2) {
        println(s"Max memory: ${maxMemory} MB")
        println("File1\t:\t" + eFile1)
        println("gtFile\t:\t" + gtFile)
    }

    val eReader1 = new EntitySerializationReader(eFile1)
    val profiles1 = eReader1.getEntityProfiles.asScala.toArray
    if (Config.print2) System.out.println("Input Entity Profiles1\t:\t" + profiles1.size)

    val gtReader = new GtSerializationReader(gtFile)
    val dpA = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null))
    val dpB = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null))
    if (Config.print2) System.out.println("Existing Duplicates\t:\t" + dpA.getDuplicates.size)

      /** STEP 2. functional stages **/
    val tokenizer = new Tokenizer
    val tokenBlocker = Blocking.apply(Config.blocker, profiles1.size, 0, Config.cuttingRatio, Config.filteringRatio)
    val compCleaner = ComparisonCleaning.apply(Config.ccMethod,Config.supervisedApproach,dpA, Config.thSupervised)
    val compMatcher = new JSMatcher
    val proCollectorA = new ProgressiveCollector(t0, System.currentTimeMillis(), dpA, Config.print)
    val proCollectorB = new ProgressiveCollector(t0, System.currentTimeMillis(), dpB, Config.print)
    //val blockGhoster = new BlockGhosting(Config.filteringRatio)
    val compGeneration = new CompGeneration

    val storeModel = new StoreModel

    val positionalFilter =new PositionalFilter
    tokenBlocker.setModelStoring(!smBool)

    val t1 = System.currentTimeMillis()

    val csv = new CsvWriter("time")


    val writer = new  CsvWriter(header="x,y,z\n")

    /** STEP 3. iterative computation -> (Assume CCER) **/
    var i = 0
    val n = profiles1.length
    var duplisTotal = 0.0
    println("\n*** Registros = " + n + "\n")
    while (i < n)
    {

        var comps1 = List[Comparison]()
        var t = 0L
        var tTotalRegistro = 0.0D


//        if (i == 27674) {
//          //  println("string é obj1 11003: ")
////            println("profile ", profiles1(27674).getAttributes.asScala)
////            println("profile ", profiles1(3031).getAttributes.asScala)
//            //println(" string é cmp ", comps1(61).e2Model.getItemsFrequency.keySet())
//        }


        //tInicial = System.currentTimeMillis() //Retorna um inteiro long em millesegundos
        t = System.nanoTime()
        val (id1, obj1) = tokenizer.execute(i, 0, profiles1(i))
        //obj1 tokeniza registros por token e guarda frequencia dos tokens em cada reqistro
        tTokenizer += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6
        //println("\nTokens Chaves de Blocos: " + obj1.getSignatures.size())

        t = System.nanoTime()
        val (tuple,prefix) = tokenBlocker.processPrefix(id1, obj1)
        tBlocker += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6
        /** tuple._3 contém os blocos referente ao registro de consulta **/
        //println("Numero de Blocos Pruning: " + tuple._3.size)
        //println("Numero de Blocos Filtrados: " + tuple._3.size)


        //t = System.nanoTime()
        //val bIds = blockGhoster.process(tuple._1, tuple._2, tuple._3)
        //tBlockGhosting += (System.nanoTime() - t) * 1E-6
        //tTotalRegistro += (System.nanoTime() - t) * 1E-6
        //println("Numero de Blocos Ghosting: " + bIds._3.size)


        t = System.nanoTime()
        comps1 = compGeneration.generateComparisons(tuple._1, tuple._2, tuple._3) //Sem o BlockGhosting
        //comps1 = compGeneration.generateComparisons(bIds._1, bIds._2, bIds._3)  //Com o BlockGhosting
        cBlocker += comps1.size
        tCompGeneration += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6
       // println("Pares Candidatos Gerados = " + comps1.size)



        /** Os blocos mantém apenas os ID's dos registros.
         * O storeModel é a etapa Flm que recupera os registros com os tokens antes da fase de comparação.
         * Para isso, o Flm mantém um mapa de perfil PM. PM serve como um índice para pesquisar um registro completo que foi determinado para exigir comparação com o registro de consulta processado atualmente.
         * */
        t = System.nanoTime()
        storeModel.solveUpdate(id1, obj1)
        comps1 = storeModel.solveComparisons(comps1,tuple._4)
        tStoreModel += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6


        for (i <-comps1) {
            i.blockingThreshold=prefix
        }

        //faz a pouda por prefixo
        comps1=positionalFilter.execute(comps1)

        t = System.nanoTime()
        if (comps1.size != 0)
            comps1=compCleaner.removeRedundantComparisons(comps1)

        proCollectorA.execute(comps1)
//        tCollector += (System.nanoTime() - t) * 1E-6
//        tTotalRegistro += (System.nanoTime() - t) * 1E-6

        t = System.nanoTime()
        comps1 = compCleaner.execute(comps1)
        cCompCleaner += comps1.size
        tCompCleaner += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6
        //println("Pares Candidatos Apos Meta-blocking = " + comps1.size)
        //println("Pares Candidatos Sem Redundancia = " + comps1.size) //comps1.toList

        t = System.nanoTime()
        comps1 = compMatcher.execute(comps1) //Calcula similaridade de jaccard igual a 0
        tMatcher += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6

        t = System.nanoTime()
        proCollectorB.execute(comps1)
        tCollector += (System.nanoTime() - t) * 1E-6
        tTotalRegistro += (System.nanoTime() - t) * 1E-6
        //tFinal = (System.currentTimeMillis() - tInicial)
        //  println("Duplicatas Totais = " + proCollector.getCardinaliy)
        // println("PC = " + (proCollector.getPC()))
        //duplisTotal = proCollector.getCardinaliy

        //println("\nTempo Mili = " + tFinal + " ms"  + "\n")
        //println("Tempo = " + tTotalRegistro + " ms"  + "\n")

        //val line = List[String](tTotalRegistro.toString.replace('.' , ','))
        //csv.newLine(line)
//        if (i%1000==0) {
//            println("Registro " + i + " de " + proCollector.getPC())
//        }

        i += 1
        val lisa= List[String](i.toString, compCleaner.getTotalSize().toString,(compCleaner.getTotalSize()/i).toString)
        writer.newLine(lisa)
    }

    val afterUsedMem = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    val actualMemUsed = afterUsedMem - beforeUsedMem
    println("memory used is "+ actualMemUsed/(1024*1024)  +" mb")
    writer.writeFile("./plot/"+Config.dataset1+".csv", false)
    //println("\nFrequencia dos Tokens:\n" + TokenBlocker.frequencyTokens.toString)

    //proCollector.printLast()

    println(s"\nMax memory: ${maxMemory} MB")
   // println("recall---", compCleaner.getRecall(), " ---precision--- ", compCleaner.getPrecision() , "  compCleaner.getTotalsize()  ", compCleaner.getTotalSize())
    println("\n------------------------------ \n------------------------------ \n\nTime measurements:\n")

    println("tTokenizer = " + tTokenizer.toInt + " ms")
    println("tBlocker = " + tBlocker.toInt + " ms")
    println("tBlockGhosting = " + tBlockGhosting.toInt + " ms")
    println("tCompGeneration = " + tCompGeneration.toInt + " ms")
    println("tCompCleaner = " + tCompCleaner.toInt + " ms")
    println("tStoreModel = " + tStoreModel.toInt + " ms")
    println("tMatcher = " + tMatcher.toInt + " ms")
    println("tCollector = " + tCollector.toInt + " ms")

    var tempoTotal = (tTokenizer + tBlocker + tBlockGhosting + tCompGeneration + tCompCleaner + tStoreModel + tMatcher + tCollector)

    println("\n------------------------------ \n------------------------------ \n\n*** Results: ***\n")

    println("Tempo medio registro = " + tempoTotal / i + " ms")
    println("Tempo total = " + tempoTotal + " ms") //Tempo sem impressão
    println("Duplicatas encontradas apos filtro posição = " + proCollectorA.em.toInt)
    println("Duplicatas existentes  apos filtro posição = " + proCollectorA.ec.toInt)
    println("Duplicatas encontradas apos super = " + proCollectorB.em.toInt)
    println("Duplicatas existentes  apos super = " + proCollectorB.ec.toInt)
    println("Pares candidatos blocking: " + cBlocker)
    println("Pares candidatos comparison cleaning: " + cCompCleaner)
    println("PC after filtering = " + proCollectorA.getPC)
    println("PQ after filtering = " + proCollectorA.getPQ + "\n")
    println("PC after super= " + proCollectorB.getPC)
    println("PQ after super = " + proCollectorB.getPQ + "\n")
   // csv.writeFile(Config.file, Config.append)

//        if (true) {
//            val csv = new CsvWriter("name,dr,bb+bp,bg,cg,cc,lm,co,cl,sum,RT,PC")
//            val name = dataset1+Config.cuttingRatio
//            val dr = tTokenizer
//            val bb_bp = tBlocker
//            val bg = tBlockGhosting
//            val cg = tCompGeneration
//            val cc = tCompCleaner
//            val lm = tStoreModel
//            val co = tMatcher
//            val cl = tCollector
//            val sum = dr+bb_bp+bg+cg+cc+lm+co+cl
////            val RT = (System.currentTimeMillis() - t1).toString
//            val PC = proCollector.getPC.toString
//
//            val line = List[String](name,dr.toString,bb_bp.toString,bg.toString,cg.toString,cc.toString,lm.toString,co.toString,cl.toString,sum.toString,PC)
//    println(name)
//    println(line)
//            csv.newLine(line)
//            csv.writeFile(Config.file, Config.append)
//        }

        if (true) {
            val csv = new CsvWriter("name, CoCl, ro, ff, PC, PQ, BB(T), CC(T), MA(T), O(T), OD(T), BB(C), CC(C)")

            val CoCl = Config.ccMethod
            val ro = Config.cuttingRatio.toString
            val ff = Config.filteringRatio.toString
            val p = if (smBool) "sm-" else ""
            val name = p+CoCl + "-" + ro + "-" + ff +  Config.dataset1 + "-" + Config.dataset2

            val PCA = (proCollectorA.getPC).toString
            val PQA = (proCollectorA.getPQ).toString
            val PCB = (proCollectorB.getPC).toString
            val PQB = (proCollectorB.getPQ).toString
            val BBT = (tTokenizer + tBlocker + tStoreModel).toString
            val CCT = tCompCleaner.toString
            val MAT = tMatcher.toString
            val OT = (tTokenizer+tBlocker+tCompCleaner+tMatcher).toString
            val ODT = (System.currentTimeMillis() - t0).toString
            val BBC = cBlocker.toString
            val CCC = cCompCleaner.toString

            val line = List[String](name, CoCl, "PCA", PCA,"PQA", PQA,"PCB", PCB,"PQB", PQB, "BBC ", BBC, "CCC ", CCC, "BBT", BBT, "CCT", CCT, "MAT", MAT, "OT", OT, "ODT", ODT)
            csv.newLine(line)
            csv.writeFile(Config.file, Config.append)

        }
}
