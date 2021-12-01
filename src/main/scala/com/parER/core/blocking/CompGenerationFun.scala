package com.parER.core.blocking

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

object CompGenerationFun {

  final def process(idx: Int, textModel: TokenNGrams, blocks: List[List[Int]]) = {
    (idx, textModel, blocks.flatten)
  }

  final def getComparisons(idx: Int, textModel: TokenNGrams, blocks: List[Int]) : List[Comparison] = {
    val comparisons = List.newBuilder[Comparison]
    (textModel.getDatasetId, Config.ccer) match {
      case (_, false) | (1, true) => for (i <- blocks) comparisons.addOne(Comparison(i, null, idx, textModel))
      case(0, true) => for (i <- blocks) comparisons.addOne(Comparison(idx, textModel, i, null))
    }
    comparisons.result()
  }

  final def generateComparisons(idx: Int, textModel: TokenNGrams, blocks: List[List[Int]]) : List[Comparison] = {
    val comparisons = List.newBuilder[Comparison]
    (textModel.getDatasetId, Config.ccer) match {
      case (_, false) | (1, true) => for (block <- blocks; i <- block) comparisons.addOne(Comparison(i, null, idx, textModel))
      case(0, true) => for (block <- blocks; i <- block) comparisons.addOne(Comparison(idx, textModel, i, null))
    }
    comparisons.result()
  }
}
