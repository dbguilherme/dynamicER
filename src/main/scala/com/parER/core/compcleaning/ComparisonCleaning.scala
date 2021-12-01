package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

trait ComparisonCleaning {
  def execute(comparisons: List[Comparison]) : List[Comparison]
  def execute(id: Int, model: TokenNGrams, ids: List[Int]) : (Int, TokenNGrams, List[Int])
}

object ComparisonCleaning {
  def apply(name: String) = name match {
    case "hs" => new HSCompCleaner
    case "wnp" => new WNPCompCleaner
    case "wnp2" => new WNP2CompCleaner
    case "cnp" => new CNPCompCleaner
  }
}