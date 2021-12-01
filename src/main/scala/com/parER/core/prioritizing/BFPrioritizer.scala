package com.parER.core.prioritizing

import com.parER.core.matching.BFMatcher
import com.parER.datastructure.Comparison

import scala.collection.mutable

class BFPrioritizer(val numberOfBits: Long, val numberOfHashes: Int) extends Prioritizer {

  val pq = mutable.PriorityQueue()(Ordering.by[Comparison, Double](_.sim))
  val bfMatcher = new BFMatcher(numberOfBits, numberOfHashes)

  override def execute(comparisons: List[Comparison]) = {
    val cmps = bfMatcher.execute(comparisons)
    pq.addAll(cmps)
  }

  override def execute(comparisons: List[Comparison], threshold: Double) = {
    val cmps = bfMatcher.execute(comparisons)
    val (left, right) = cmps.partition(_.sim < threshold )
    pq.addAll(left)
    right
  }

  override def get() = pq.dequeueAll.toList

  override def get(k: Int) = {
    val comparisons = List.newBuilder[Comparison]
    for (i <- 0 until k if !pq.isEmpty)
      comparisons.addOne(pq.dequeue())
    comparisons.result()
  }

  def get(predicate: Comparison => Boolean) = {
    val comparisons = List.newBuilder[Comparison]
    var exit = false
    while (!pq.isEmpty & !exit) {
      val c = pq.dequeue()
      comparisons.addOne(c)
      if (!predicate(c))
        exit = true
    }
    comparisons.result()
  }

  override def hasComparisons(): Boolean = !pq.isEmpty
}
