package com.parER.core.compcleaning

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

import scala.collection.mutable

class HSCompCleaner  extends ComparisonCleaning {


  val ccer = Config.ccer

  override def execute(comparisons: List[Comparison]) = {
    if (comparisons.size == 0)
      comparisons
    else {
      removeRedundantComparisons(comparisons)
    }
  }

  override def execute(id: Int, model: TokenNGrams, ids: List[Int]) = {
    if (ids.size == 0)
      (id, model, List[Int]())
    else {
      (id, model, removeRedundantIntegers(ids).keys.toList)
    }
  }

  def removeRedundantIntegers(ids: List[Int]) : Map[Int, Int] = {
    if (ids.size == 1) {
      List((ids.head, 1)).toMap
    } else if (ids.size == 0) {
      List[(Int, Int)]().toMap
    } else {
      val builder = mutable.HashMap.empty[Int, Int]
      val seen = mutable.HashSet.empty[Int]
      val it = ids.iterator
      var different = false
      while (it.hasNext) {
        val next = it.next()
        val id = next
        if (seen.add(id)) {
          builder(id) = 1
        } else {
          builder(id) += 1
          different = true
        }
      }
      builder.result().toMap
    }
  }

  def removeRedundantComparisons(comparisons: List[Comparison]) = {
    // In Dirty ER: for all c in comparison c.e2 is equal
    if (!ccer | comparisons.head.e2 == comparisons.last.e2)
      distinctAndCount(comparisons, _.e1, 0)
    else if (ccer && comparisons.head.e1 == comparisons.last.e1)
      distinctAndCount(comparisons, _.e2, 1)
    else
    distinctAndCount(comparisons, _.e1, 0)
  }

  def getRecall ():Double={0.0 }


  private def distinctAndCount(comparisons: List[Comparison], f: Comparison => Int, idx: Int) = {
    if (comparisons.size == 1) {
      comparisons.head.sim = 1
      comparisons
    } else if (comparisons.size == 0) {
      comparisons
    } else {
      val builder = mutable.HashMap.empty[Int, Comparison]
      val seen = mutable.HashSet.empty[Int]
      val it = comparisons.iterator
      var different = false
      while (it.hasNext) {
        val next = it.next()
        val id = f(next)
        if (seen.add(id)) {
          next.sim = 1
          builder(id) = next

        } else {
          builder(id).sim += 1.0

          //salva o menor blocking key
          if (builder(id).blockingKey>next.blockingKey)
            builder(id).blockingKey=next.blockingKey
          //builder(id).blockSize += next.blockSize
          different = true
        }
      }
      if (different) builder.values.toList else comparisons
    }
  }

//  private def distinctAndCount(comparisons: List[Comparison], f: Comparison => Int, idx: Int) = {
//    if (comparisons.size == 1) {
//      comparisons.head.counters(0) = 1
//      comparisons
//    } else if (comparisons.size == 0) {
//      comparisons
//    } else {
//      val builder = mutable.HashMap.empty[Int, Comparison]
//      val seen = mutable.HashSet.empty[Int]
//      val it = comparisons.iterator
//      var different = false
//      while (it.hasNext) {
//        val next = it.next()
//        val id = f(next)
//        if (seen.add(id)) {
//          next.counters(idx) = 1
//          builder(id) = next
//        } else {
//          builder(id).counters(idx) += 1
//          different = true
//        }
//      }
//      if (different) builder.values.toList else comparisons
//    }
//  }

  override def getLabelCost(): Int = 0

  def getPrecision(): Double = {
    0.0
  }
  def getTotalSize(): Int = {
    0
  }
}
