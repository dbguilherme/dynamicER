package com.parER.core.matching

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.BagModel

import java.util
import java.util.{HashSet, Set}

class JSMatcher extends Matcher {
  override def execute(comparisons: List[Comparison]) = {
    if (comparisons == null)
      comparisons
    else {
      var v= comparisons.map(cmp => {
        cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model)
        cmp
      })
      v= v.filter(_.sim < 0.5)
      v
    }
  }
    override def execute(cmp: Comparison): Comparison = {
    cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model)
    cmp
  }


//  protected def getJaccardSimilarity(oModel: BagModel): Float = {
//    val commonKeys: util.Set[String] = new util.HashSet[String](itemsFrequency.keySet)
//    commonKeys.retainAll(oModel.getItemsFrequency.keySet)
//    val numerator: Int = commonKeys.size
//    val denominator: Int = itemsFrequency.size + oModel.getItemsFrequency.size - numerator
//    numerator / denominator
//  }
  override def getName(): String = "JSMatcher"
}
