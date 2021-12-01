package com.parER.core.clustering

import com.parER.datastructure.Comparison
import org.scify.jedai.datamodel.EquivalenceCluster

trait Clustering {
  def getDuplicates(comparisons: List[Comparison]): Array[EquivalenceCluster]
}
