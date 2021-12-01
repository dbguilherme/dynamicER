package com.parER.core.blocking

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

import scala.collection.mutable.ListBuffer

object BlockGhostingFun {

  final def process(id: Int, model: TokenNGrams, blocks: List[List[Int]], ff: Double) = {
    (id, model, getBlocks(blocks, ff))
  }

  final def getBlocks(blocks: List[List[Int]], ff: Double) = {
    if (blocks.size > 0){
      val minSize = blocks.foldLeft(blocks(0).size){ (min, e) => math.min(min, e.size) }
      blocks.filter(b => (b.size+1)*ff < minSize+1)
    } else
      blocks
  }

  final private def generateComparisons(idx: Int, textModel: TokenNGrams, blocks: ListBuffer[List[Int]]) = {
    val comparisons = List.newBuilder[Comparison]
    (textModel.getDatasetId, Config.ccer) match {
      case (_, false) | (1, true) => for (block <- blocks; i <- block) comparisons.addOne(new Comparison(i, null, idx, textModel))
      case(0, true) => for (block <- blocks; i <- block) comparisons.addOne(new Comparison(idx, textModel, i, null))
    }
    comparisons.result()
  }

}
