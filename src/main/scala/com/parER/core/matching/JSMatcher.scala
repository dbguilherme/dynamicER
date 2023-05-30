package com.parER.core.matching

import com.parER.datastructure.Comparison

class JSMatcher extends Matcher {
  override def execute(comparisons: List[Comparison]) = {
    if (comparisons == null)
      comparisons
    else
      comparisons.map(cmp => {
        var a=cmp.sim
        //cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model) //bug no codigo .java Jedai, divisao de inteiros resulta em inteiro
        cmp.sim=a
        cmp
      })
  }


  override def execute(cmp: Comparison): Comparison = {
    cmp.sim = cmp.e1Model.getSimilarity(cmp.e2Model)
    cmp
  }

  override def getName(): String = "JSMatcher"
}
