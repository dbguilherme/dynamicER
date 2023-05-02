package com.parER.datastructure

import com.parER.core.Config
import org.scify.jedai.textmodels.TokenNGrams

import scala.collection.mutable.{HashMap, ListBuffer}

class InvertedIndex {

  val invertedIndex = Array(
    new HashMap[String, ListBuffer[Int]]() { override def apply(key: String) = super.getOrElseUpdate(key, ListBuffer())},
    new HashMap[String, ListBuffer[Int]]() { override def apply(key: String) = super.getOrElseUpdate(key, ListBuffer())})
  val modelIndex = Array( HashMap[Int, TokenNGrams](), HashMap[Int, TokenNGrams]())
  val ccer = Config.ccer
  var maiorBloco = 0L

  def update(idx: Int, textModel: TokenNGrams, textModelTokens: List[String], storeModel: Boolean = true): Unit = {
    val (mi, ii) = getIndexesForUpdate(textModel.getDatasetId)
    for (token <- textModelTokens) {
      ii(token) += idx //Para cada bloco selecionado insere o id do registro de consulta
      //if ( ii(token).size > maiorBloco)
      //  maiorBloco = ii(token).size
    }
    //println("Tamanho Indice Invertido: " + ii.size)
    //println("Tamanho Maior Bloco: " + maiorBloco)
    if (storeModel)
      mi(idx) = textModel //Atualiza o textModel do registro inserido (idx)
  }

  // TODO guarantee correct order: e1(0) - e2(1) is a different comparison than e2(0) - e1(1)
  def generate(idx: Int, textModel: TokenNGrams, textModelTokens: List[String], storeModel: Boolean = true) = {
    val comparisons = List.newBuilder[Comparison]
    val dId = textModel.getDatasetId
    val (mi, ii) = getIndexesForRetrieve(dId)
    if (storeModel)
      (dId, ccer) match {
        case (_, false) | (1, true) => for (token <- textModelTokens; i <- ii(token)) comparisons.addOne(Comparison(i, mi(i), idx, textModel))
        case(0, true) => for (token <- textModelTokens; i <- ii(token)) comparisons.addOne(Comparison(idx, textModel, i, mi(i)))
      }
    else {
      (dId, ccer) match {
        case (_, false) | (1, true) => for (token <- textModelTokens; i <- ii(token)) comparisons.addOne(Comparison(i, null, idx, textModel))
        case(0, true) => for (token <- textModelTokens; i <- ii(token)) comparisons.addOne(Comparison(idx, textModel, i, null))
      }
    }
    comparisons.result()
  }

  def getModelIndexView(idx: Int, dId: Int, keys: List[String]) = {
    val (mi, ii) = getIndexesForRetrieve(dId)
    var buff = new ListBuffer[Int]
    for (token <- keys)
      buff ++= ii(token)
    val indices = buff.toSet
    mi.filter(x => indices.contains(x._1)).toMap
  }

  def getBlocks(idx: Int, dId: Int, keys: List[String]) = {
    val (mi, ii) = getIndexesForRetrieve(dId)
    var hm = ii.filter(x => keys.contains(x._1))
    hm
  }

  def getBlock(idx: Int, dId: Int, token: String) = {
    val (mi, ii) = getIndexesForRetrieve(dId)
    ii(token)
  }

  def associatedBlocks(idx: Int, dId: Int, textModelTokens: List[String], predicate: String => Boolean) = {
    val (mi, ii) = getIndexesForRetrieve(dId)
    for (t <- textModelTokens ; if predicate(t)) yield (t, ii(t))
  }

  def partitionedAssociatedBlocks(idx: Int, dId: Int, keys: List[String]) = {
    val l, l0 = List.newBuilder[(String, ListBuffer[Int])]
    val (mi, ii) = getIndexesForRetrieve(dId)
    // mi = modelIndex - ii = invertedIndex

    var i = 0
    for (t <- keys)
    {
      i += 1
      val block = ii(t)
      if (block.size == 0)
        l0.addOne((t, block)) //Insere na lista l0 de blocos com tamanho zero (associatedBlocksWithZeroSize)
      else
        l.addOne((t, block)) //Insere bloco em l
      /** Block Pruning retirado **/
    }
    (l.result(), l0.result())
  }

  def partitionedAssociatedBlocksOriginal(idx: Int, dId: Int, keys: List[String], predicate: String => Boolean, partitionPredicate: ListBuffer[Int] => Boolean) = {
    val l, l0, r = List.newBuilder[(String, ListBuffer[Int])]
    val (mi, ii) = getIndexesForRetrieve(dId)
    // mi = modelIndex - ii = invertedIndex

    var i = 0
    for (t <- keys ; if predicate(t)) //Se não estiver na lista de criminalTokens (predicate)
    {
      i += 1
      val block = ii(t)
      if (block.size == 0)
        l0.addOne((t, block)) //Insere na lista l0 de blocos com tamanho zero (associatedBlocksWithZeroSize)
      //Se o tamanho do bloco+1 for menor que maxBlockSize (partitionPredicate) insere bloco em l (associatedBlocks).
      //Se não, insere em r para remover (blocksToRemove -> criminalTokens)
      else (if (partitionPredicate(block)) l else r).addOne((t,block))
      //else l.addOne((t,block)) /** Insere bloco. Nunca remove (BlockPruning) **/
    }
    (l.result(), l0.result(), r.result())
  }

  def remove(tok: String) = {
    invertedIndex(0).remove(tok)
    invertedIndex(1).remove(tok)
  }

  private def getIndexesForRetrieve(dId: Int) = {
    (dId, ccer) match {
      case (_, false) => (modelIndex(0), invertedIndex(0))
      case (0, true) => (modelIndex(1), invertedIndex(1))
      case (1, true) => (modelIndex(0), invertedIndex(0))
    }
  }

  private def getIndexesForUpdate(dId: Int) = {
    (dId, ccer) match {
      case (_, false) => (modelIndex(0), invertedIndex(0))
      case (1, true) => (modelIndex(1), invertedIndex(1))
      case (0, true) => (modelIndex(0), invertedIndex(0))
    }
  }
}
