package com.parER.core.matching

import com.parER.datastructure.Comparison

class JSMatcher extends Matcher {
  override def execute(comparisons: List[Comparison]) = {
    if (comparisons == null)
      comparisons
    else
      comparisons.map(cmp => {
        cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model)
        cmp
      })
  }

  override def execute(cmp: Comparison): Comparison = {
    cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model)
    cmp
  }

  override def getName(): String = "JSMatcher"
}
