package com.parER.core.blocking

import org.scify.jedai.textmodels.TokenNGrams

import scala.collection.mutable

class TokenBlockerRefiner(size1: Int, size2: Int = 0, ro: Double = 0.005, ff: Double = 0.01) extends TokenBlocker {

  // Tokens to blacklist
  val criminalTokens = mutable.HashSet[String]()
  val maxBlockSize = Array(ro*size1, ro*size2)

  //println("ro: " + ro + "   ff: " + ff)
  println(s"TokenBlockerRefiner mbs1=${maxBlockSize(0)} ;; mbs2=${maxBlockSize(1)}")
  //System.in.read()
  // Modify TokenBlocker.invertedIndex and build the list of comparisons out of it
  // Performs block cutting + block filtering + block purging if memory is saturated
  // TODO check correctness
  override def execute(idx: Int, textModel: TokenNGrams) = {
    import scala.jdk.CollectionConverters._
    val textModelTokens = textModel.getSignatures.asScala.toList
    execute(idx, textModel, textModelTokens)
  }

  override def execute(idx: Int, textModel: TokenNGrams, keys: List[String]) = {
    val dId = textModel.getDatasetId
    val (associatedBlocks, associatedBlocksWithZeroSize) = getBlocks(idx, dId, keys)

    // generate comparisons
    val comparisons = invertedIndex.generate(idx, textModel, associatedBlocks.map(e => e._1), modelStoring)
    invertedIndex.update(idx, textModel, associatedBlocksWithZeroSize.map(e=> e._1) ++ associatedBlocks.map(e => e._1), modelStoring)

    //if (comparisons.size > 0)
    //  comparisons.head.counters(0) = associatedBlocks.size

    comparisons
  }

  override def process(idx: Int, textModel: TokenNGrams) = {
    import scala.jdk.CollectionConverters._

    //Pega somente as assinaturas para chaves (keys) de blocos
    val keys = textModel.getSignatures.asScala.toList
    //println("Keys toList: " + textModelTokens.toList)

    process(idx, textModel, keys)
  }

  override def processPrefix(idx: Int, textModel: TokenNGrams) = {
    import scala.jdk.CollectionConverters._

    //Pega somente as assinaturas para chaves (keys) de blocos
    val keys = textModel.getSignatures.asScala.toList
    //println("Keys toList: " + textModelTokens.toList)

    processPrefix(idx, textModel, keys)
  }
  override def processPrefix(idx: Int, textModel: TokenNGrams, keys: List[String]) = {

    // Cria um map para ordenar frequencia das chaves do registro
    val keysFreqRegistro = collection.mutable.Map[String, Integer]()

    for (key <- keys)
    {
      //Conta a frequencia de cada chave. Registro pode ter chave repetida, mas so conta uma vez.
      if (!frequencyKeys.increment(key))
      {
        frequencyKeys.put(key, 1)
      }
      //Guarda as chaves dos registros com sua frequencia atual
      keysFreqRegistro += (key -> frequencyKeys.get(key).intValue)
    }
    if (idx == 27621)
      print("teste ")
    //println("\nFrequencia das Chaves:\n" + frequencyKeys.toString)
    //System.in.read()

    /** COMO CALCULAR O PREFIXO (prefix) DO REGISTRO???
     * ACHAR NUMERO DE REGISTROS (n) DE ENTRADA QUE FUNCIONE BEM
     **/

    var prefix = 0
    val n = 500
    if(idx < n)
      prefix = keysFreqRegistro.size
    else
    {
      val p = (keysFreqRegistro.size / 2).toInt
      //val p=5
      if(p >= 1)
        prefix = p
      else
        prefix = 1
    }

    /** ** **/

    // Ordena o map de chaves pelo valor de frequencia
    val keysOrdFreq = keysFreqRegistro.toSeq.sortBy(_._2)

    //Seleciona as chaves (ordenadas pela frequencia) pelo tamanho do prefixo
    val keysPrefix = keysOrdFreq.take(prefix)

    //Coloca todas as chaves em uma lista de Strings (sem a frequencia)
    val keysFinal = keysPrefix.map(_._1).toList
    //println(keysFinal)

    val dId = textModel.getDatasetId
    //println("dId: " + dId) // Exemplo: mbs1=maxBlockSize(0) mbs2=maxBlockSize(1) - 0 para dirtyER (1 bd)
    // get blocks and apply block cutting and first step of block filtering
    val (associatedBlocks, associatedBlocksWithZeroSize) = getBlocks(idx, dId, keysFinal)
    // get view of the hash map for later generation of comparisons
    //val view = invertedIndex.getModelIndexView(idx, textModel.getDatasetId, associatedBlocks.map(e => e._1))
    // build tuple and update inverted index data structure


    //corta os blocos com tamanho maior que 10 TESTE
    //val xxx = associatedBlocks.filter(_._2.size<10)
    val blocks = associatedBlocks.map(a => a._2.toList)
    val tuple = (idx, textModel, blocks,associatedBlocks)
    //Insere o registro de consulta nos blocos selecionados
    invertedIndex.update(idx, textModel, associatedBlocksWithZeroSize.map(e=> e._1) ++ associatedBlocks.map(e => e._1), modelStoring)

    (tuple)
  }
  //Fim do process

  def getBlocks(idx: Int, dId: Int, keys: List[String]) =
  {
    // Block cutting: cutted blocks are also purged
    //val allAssociatedBlocks = invertedIndex.associatedBlocks(idx, dId, textModelTokens, t => !criminalTokens.contains(t))
    //var (associatedBlocks, blocksToRemove) = allAssociatedBlocks.partition(_._2.size+1 < maxBlockSize(dId))
    var (associatedBlocks, associatedBlocksWithZeroSize) = invertedIndex.
      partitionedAssociatedBlocks(idx, dId, keys)

    //println("Blocos Sem Registros: " + associatedBlocksWithZeroSize.size)
    //println(associatedBlocksWithZeroSize.toList)
    //println("Blocos Filtrados: " + associatedBlocks.size)
    //println(associatedBlocks.toList) //Não contém blocos de tamanho 1

    (associatedBlocks, associatedBlocksWithZeroSize)
  }

  /*override*/ def processOriginal(idx: Int, textModel: TokenNGrams, keys: List[String]) =
  {
    val dId = textModel.getDatasetId
    //println("dId: " + dId) // Exemplo: mbs1=maxBlockSize(0) mbs2=maxBlockSize(1) - 0 para dirtyER (1 bd)
    // get blocks and apply block cutting and first step of block filtering
    val (associatedBlocks, associatedBlocksWithZeroSize) = getBlocksOriginal(idx, dId, keys)
    // get view of the hash map for later generation of comparisons
    //val view = invertedIndex.getModelIndexView(idx, textModel.getDatasetId, associatedBlocks.map(e => e._1))
    // build tuple and update inverted index data structure
    val blocks = associatedBlocks.map(e => e._2.toList)
    val tuple = (idx, textModel, blocks)
    //Insere o registro de consulta nos blocos selecionados
    invertedIndex.update(idx, textModel, associatedBlocksWithZeroSize.map(e=> e._1) ++ associatedBlocks.map(e => e._1), modelStoring)

    tuple
  }

  def getBlocksOriginal(idx: Int, dId: Int, keys: List[String]) =
  {
    // Block cutting: cutted blocks are also purged
    //val allAssociatedBlocks = invertedIndex.associatedBlocks(idx, dId, textModelTokens, t => !criminalTokens.contains(t))
    //var (associatedBlocks, blocksToRemove) = allAssociatedBlocks.partition(_._2.size+1 < maxBlockSize(dId))
    var (associatedBlocks, associatedBlocksWithZeroSize, blocksToRemove) = invertedIndex.
      partitionedAssociatedBlocksOriginal(idx, dId, keys,
        !criminalTokens.contains(_), _.size+1 < maxBlockSize(dId))
    for ((t,b) <- blocksToRemove)
    {
      //println("*** ENTROU AQUI Block Pruning Remove Blocos do Indice Invertido ***")
      //System.in.read()
      //Para não remover blocos habilitar //else l.addOne((t,block)) em partitionedAssociatedBlocks em InvertedIndex
      invertedIndex.remove(t)
      criminalTokens += t
    }

    //println("CriminalTokensAtual: " + blocksToRemove.size)
    //println("CriminalTokensTotal (Blocos Excluidos): " + criminalTokens.size)
    //println("CriminalTokensTotal: " + criminalTokens.size + " - " + criminalTokens.toList)
    //println("Blocos Sem Registros: " + associatedBlocksWithZeroSize.size)
    //println("Blocos Sem Registros: " + associatedBlocksWithZeroSize.size + " - " + associatedBlocksWithZeroSize.toList)
    //println("Numero de Blocos Com Registros: " + (associatedBlocks.size + blocksToRemove.size))
    //println(associatedBlocks.toList) //Não contém blocos de tamanho 1 e blocos (tokens) removidos no Pruning

    //** block filtering (Ou será block ghosting?) **Esta parte estava habilitada. Tirei pq é repetido. CONFERIR
    if (associatedBlocks.size > 0)
    {
      val minSize = associatedBlocks.foldLeft(associatedBlocks(0)._2.size){ (min, e) => math.min(min, e._2.size) }
      associatedBlocks = associatedBlocks.filter{ case(t,b) => (b.size+1)*ff < minSize+1 } // TODO keep top-k ?
    }

    //println("Blocos Apos Filtering: " + associatedBlocks.toList)
    //println("Blocos Apos Ghosting: " + associatedBlocks.toList)

    // block purging if memory is saturated
    // TODO
    (associatedBlocks, associatedBlocksWithZeroSize)
  }
}
