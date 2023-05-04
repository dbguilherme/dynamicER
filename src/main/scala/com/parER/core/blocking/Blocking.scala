package com.parER.core.blocking

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

trait Blocking {
  def execute(idx: Int, textModel: TokenNGrams) : List[Comparison]
  def execute(idx: Int, textModel: TokenNGrams, keys: List[String]) : List[Comparison]
  def setModelStoring(value: Boolean) : Unit
  def process(idx: Int, textModel: TokenNGrams) : (Int, TokenNGrams, List[List[Int]])
  def process(idx: Int, textModel: TokenNGrams, keys: List[String]) : (Int, TokenNGrams, List[List[Int]])
  def processPrefix(idx: Int, textModel: TokenNGrams, keys: List[String]) : ((Int, TokenNGrams, List[List[Int]], List[(String)]), Int)
  def processPrefix(idx: Int, textModel: TokenNGrams) : ((Int, TokenNGrams, List[List[Int]], List[(String)]), Int)
}

object Blocking {
  def apply(name: String, size1: Int, size2: Int = 0, ro: Double = 0.005, ff: Double = 0.01) = name match {
    case "tb" => new TokenBlocker()
    case "tbr" => new TokenBlockerRefiner(size1, size2, ro, ff)
  }
}