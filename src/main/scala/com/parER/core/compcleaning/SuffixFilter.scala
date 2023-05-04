package com.parER.core.compcleaning
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

class SuffixFilterFilter extends ComparisonCleaning{

  def filter(comparisons: List[Comparison]) = {
    for (c<-comparisons) yield {
      val e1 = c.e1
      val e2 = c.e2
//      val e1Suffix = e1.substring(e1.lastIndexOf(" ")+1)
//      val e2Suffix = e2.substring(e2.lastIndexOf(" ")+1)
//      if (e1Suffix == e2Suffix) {
//        c
//      } else {
//        Comparison(c.id, e1, e2, 0.0)
//      }
    }
  }

  override def execute(comparisons: List[Comparison]): List[Comparison] = {
    if (comparisons.size == 0)
      comparisons
    else {
      var cmps = filter(comparisons)
      // val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
      //  cmps = cmps.filter(_.sim >= w)



    }
    comparisons
  }

  override def execute(id: Int, model: TokenNGrams, ids: List[Int]): (Int, TokenNGrams, List[Int]) = ???

  override def getLabelCost(): Int = ???
}
