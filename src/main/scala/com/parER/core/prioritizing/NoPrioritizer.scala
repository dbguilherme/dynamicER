package com.parER.core.prioritizing
import com.parER.datastructure.Comparison

import scala.collection.mutable

class NoPrioritizer extends Prioritizer {

  var buffer = mutable.Queue[Comparison]()
  //val bfMatcher = new BFMatcher(1024, 1)

  override def execute(comparisons: List[Comparison]): Unit = {
    buffer ++= comparisons
  }

  override def execute(comparisons: List[Comparison], threshold: Double): List[Comparison] = {
    comparisons
  }

  override def get(): List[Comparison] = buffer.dequeueAll(_ => true).toList

  override def get(k: Int) = buffer.dequeueAll(_ => true).toList // should not be called

  override def hasComparisons(): Boolean = !buffer.isEmpty
}
