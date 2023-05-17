package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

trait ComparisonCleaning {
  def execute(comparisons: List[Comparison]): List[Comparison]

  def execute(id: Int, model: TokenNGrams, ids: List[Int]): (Int, TokenNGrams, List[Int])

  //function to return the variable labelcost
  def getLabelCost(): Int
}

object ComparisonCleaning {
  def apply(name: String, supervisedApproach: Int,dp: AbstractDuplicatePropagation, thSupervised: Double=0.0) = name match {
    //case "hs" => new HSCompCleaner(dp)
    case "wnp" => new WNPCompCleaner(dp,supervisedApproach)
    case "wnp2" => new WNP2CompCleaner(dp,supervisedApproach)
    case "wnp3" => new WNP3CompCleaner(dp,supervisedApproach, thSupervised)
    case "cnp" => new CNPCompCleaner(dp,supervisedApproach)
  }


}