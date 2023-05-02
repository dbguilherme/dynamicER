package com.parER.core.blocking

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

class CompGeneration {

  def process(idx: Int, textModel: TokenNGrams, blocks: List[List[Int]]) = {
    (idx, textModel, blocks.flatten)
  }

  def getComparisons(idx: Int, textModel: TokenNGrams, blocks: List[Int]) : List[Comparison] = {
    val comparisons = List.newBuilder[Comparison]
    (textModel.getDatasetId, Config.ccer) match {
      case (_, false) | (1, true) => for (i <- blocks) comparisons.addOne(Comparison(i, null, idx, textModel))
      case(0, true) => for (i <- blocks) comparisons.addOne(Comparison(idx, textModel, i, null))
    }
    comparisons.result()
  }

  def generateComparisons(idx: Int, textModel: TokenNGrams, blocks: List[List[Int]]) : List[Comparison] = {
    val comparisons = List.newBuilder[Comparison]
    (textModel.getDatasetId, Config.ccer) match {
      case (_, false) | (1, true) => {
              var j=1;
              for (block <- blocks) {

                for  (i <- block)
                  {comparisons.addOne(Comparison(i, null, idx, textModel,blockSize=blocks.size,blockingKey = j))
                  }
                 j+=1
              }
      }
      case(0, true) => for (block <- blocks; i <- block) comparisons.addOne(Comparison(idx, textModel, i, null,blockSize=blocks.size))
    }
    comparisons.result()
  }

}
