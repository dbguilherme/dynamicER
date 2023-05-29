package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

class PositionalFilter extends ComparisonCleaning{
  var numero=0
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
    comparisons
  }

  override def execute(comparisons: List[Comparison]): List[Comparison] = {
   // print( "before positional ", comparisons.size)

    if (comparisons.size == 0)
      comparisons
    else {
      val clean_comparisons = List.newBuilder[Comparison]
      for (c <- comparisons){
        var count=0
        var list =c.e1Model.getItemsFrequency.keySet()
        for (key<-c.prefixString){
          if (list.contains(key)){
            count+=1
          }
        }
        var prefixThreshold=2
        if (count>=prefixThreshold)//(c.blockingThreshold/4))  //Limiar é o tamanho do prefixo /2
           clean_comparisons.addOne(c)
      }

      clean_comparisons.result()


    }
  }

  override def execute(id: Int, model: TokenNGrams, ids: List[Int]): (Int, TokenNGrams, List[Int]) = ???

  override def getLabelCost(): Int = ???

   def getTotalSize(): Int = ???
}
