package com.parER.core.prioritizing

import com.parER.core.matching.SchemeJSMatcher
import com.parER.datastructure.Comparison

import scala.collection.mutable

class PpsPrioritizer(val kMax: Int) extends Prioritizer {

  val duplicationLikelihood = mutable.HashMap[Int, Double]().withDefaultValue(0.0)
  //val distinctNeighbors = mutable.HashMap[Int, Int]().withDefaultValue(0)
  //val pq = mutable.SynchronizedPriorityQueue()(Ordering.by[Comparison, Double](_.sim))
  val pq = mutable.PriorityQueue()(Ordering.by[Comparison, Double](_.sim))
  val matcher = new SchemeJSMatcher
  var totSim = 0.0

  override def execute(comparisons: List[Comparison]) = {
    if (comparisons.size != 0) {
      for (cmp <- comparisons) {
        matcher.execute(cmp)
      }
    }
  }

  // TODO this ignore the double threshold, search for a better solution
  override def execute(comparisons: List[Comparison], threshold: Double) = {
    if (comparisons.size == 0)
      comparisons
    else {
      execute(comparisons)
      get(kMax)
    }
  }

  override def get() = pq.dequeueAll.toList

  override def get(k: Int) = {
    val comparisons = List.newBuilder[Comparison]
    for (i <- 0 until k if !pq.isEmpty)
      comparisons.addOne(pq.dequeue())
    comparisons.result()
  }

  def update(comparisons: List[Comparison]) = {

  }

  def getOthers() = {
    pq.toList
  }

  def reset() = {
    pq.clear()
  }

  def get(predicate: Comparison => Boolean) = {
    null
  }

  override def hasComparisons(): Boolean = !pq.isEmpty
}
