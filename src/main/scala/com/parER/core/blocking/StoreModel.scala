package com.parER.core.blocking

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

import scala.collection.mutable.HashMap

class StoreModel(size1: Int = 16, size2: Int = 16) {

  println("Initial size1: " + size1)
  println("Initial size2: " + size2)

  val modelIndex = Array(
      HashMap.newBuilder[Int,TokenNGrams](2*size1, 0.75).result(),
      HashMap.newBuilder[Int,TokenNGrams](2*size2, 0.75).result())

  val ccer = Config.ccer

  def solveComparisons(comparisons: List[Comparison]) = {
    if (comparisons.size > 0) {
      val head = comparisons.head
      val cmps = if (head.e1Model != null) {
        val mi = getIndexForRetrieve(head.e1Model.getDatasetId)
        comparisons.map(c => new Comparison(c.e1, c.e1Model, c.e2, mi(c.e2),blockingKey = c.blockingKey))
      } else {
        val mi = getIndexForRetrieve(head.e2Model.getDatasetId)
        comparisons.map(c => new Comparison(c.e1, mi(c.e1), c.e2, c.e2Model,blockSize=c.blockSize,blockingKey = c.blockingKey))
      }
      cmps
    } else
      comparisons
  }


  def solveComparisons(comparisons: List[Comparison], prefixString:List[String]) = {
    if (comparisons.size > 0) {
      val head = comparisons.head

    //  val keysFreqRegistro = collection.mutable.Map[String, Integer]()
      val cmps = if (head.e1Model != null) {
        val mi = getIndexForRetrieve(head.e1Model.getDatasetId)
        comparisons.map(c => new Comparison(c.e1, c.e1Model, c.e2, mi(c.e2), blockingKey = c.blockingKey))
      } else {
        val mi = getIndexForRetrieve(head.e2Model.getDatasetId)

        //cria o prefixo ordenado das chaves do registro
//        for (c<-comparisons){
//          var keys = c.e2Model.getItemsFrequency.keySet().toArray().mkString(",")
//          println("value ", keys)
//          for (key <- keys) {
//            //Guarda as chaves dos registros com sua frequencia atual
//            keysFreqRegistro += (key -> frequencyKeys.get(key).intValue)
//            }
//          val keysOrdFreq = keysFreqRegistro.toSeq.sortBy(_._2)
//        }
//        for (key <- keys) {
//          //Conta a frequencia de cada chave. Registro pode ter chave repetida, mas so conta uma vez.
//          if (!frequencyKeys.increment(key)) {
//            frequencyKeys.put(key, 1)
//          }
//          //Guarda as chaves dos registros com sua frequencia atual
//          keysFreqRegistro += (key -> frequencyKeys.get(key).intValue)
//        }
        comparisons.map(c => new Comparison(c.e1, mi(c.e1), c.e2, c.e2Model, blockSize = c.blockSize, blockingKey = c.blockingKey, prefixString=prefixString))
      }
      cmps
    } else
      comparisons
  }

  def solveComparisons(comparisons: List[Comparison], dId: Int) = {
    if (comparisons.size > 0) {
      val head = comparisons.head
      val cmps = if (head.e1Model != null) {
        val mi = getIndexForRetrieve(head.e1Model.getDatasetId)
        comparisons.map(c => new Comparison(c.e1, c.e1Model, c.e2, mi(c.e2)))
      } else {
        val mi = getIndexForRetrieve(head.e2Model.getDatasetId)
        comparisons.map(c => new Comparison(c.e1, mi(c.e1), c.e2, c.e2Model))
      }
      cmps
    } else
      comparisons
  }

  def solveUpdate(id: Int, model: TokenNGrams): Unit = {
    val mi = getIndexForUpdate(model.getDatasetId)
    mi.update(id, model)
  }

  private def getIndexForRetrieve(dId: Int) = {
    (dId, ccer) match {
      case (_, false) => modelIndex(0)
      case (0, true) => modelIndex(1)
      case (1, true) => modelIndex(0)
    }
  }

  private def getIndexForUpdate(dId: Int) = {
    (dId, ccer) match {
      case (_, false) => modelIndex(0)
      case (1, true) => modelIndex(1)
      case (0, true) => modelIndex(0)
    }
  }
}
