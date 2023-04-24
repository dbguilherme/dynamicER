package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

trait ComparisonCleaning {
  def execute(comparisons: List[Comparison]) : List[Comparison]
  def execute(id: Int, model: TokenNGrams, ids: List[Int]) : (Int, TokenNGrams, List[Int])

  def getPrecision(): (Double)
}

object ComparisonCleaning {
  def apply(name: String, dp: AbstractDuplicatePropagation) = name match {
    //case "hs" => new HSCompCleaner(dp)
    case "wnp" => new WNPCompCleaner(dp)
    case "wnp2" => new WNP2CompCleaner(dp)
    case "wnp3" => new WNP3CompCleaner(dp)
    case "cnp" => new CNPCompCleaner(dp)
  }


}