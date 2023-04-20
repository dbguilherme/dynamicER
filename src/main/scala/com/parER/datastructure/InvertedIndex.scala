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

  val blockIndex =new HashMap[Int, List[Int]]() { override def apply(key: Int) = super.getOrElseUpdate(key, List())}

  def update(idx: Int, textModel: TokenNGrams, textModelTokens: List[String], storeModel: Boolean = true): Unit = {
    val (mi, ii) = getIndexesForUpdate(textModel.getDatasetId)
    for (token <- textModelTokens) {
      ii(token) += idx
      //println(ii(token))
    }
    if (storeModel)
      mi(idx) = textModel
  }
  def updateBlocking(idx: Int, tuple: (Int, TokenNGrams, List[List[Int]])) {
    for ((t)  <- tuple._3){
      blockIndex(idx)=t

    }
    println(blockIndex(idx))

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

  def partitionedAssociatedBlocks(idx: Int, dId: Int, textModelTokens: List[String], predicate: String => Boolean, partitionPredicate: ListBuffer[Int] => Boolean) = {
    val l, l0, r = List.newBuilder[(String, ListBuffer[Int])]
    val (mi, ii) = getIndexesForRetrieve(dId)
    for (t <- textModelTokens ; if predicate(t)) {
      val block = ii(t)
      if (block.size == 0) l0.addOne((t, block))
      else (if (partitionPredicate(block)) l else r).addOne((t,block))
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
