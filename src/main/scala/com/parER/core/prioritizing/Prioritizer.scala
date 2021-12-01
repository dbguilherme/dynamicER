package com.parER.core.prioritizing

import com.parER.datastructure.Comparison

trait Prioritizer {
  def execute(comparisons: List[Comparison]) : Unit
  def execute(comparisons: List[Comparison], threshold: Double) : List[Comparison]
  def get(): List[Comparison]
  def get(k : Int): List[Comparison]
  def hasComparisons(): Boolean
}

object Prioritizer {
  def apply(name: String, opt: Long = 0) = name match {
    case "no" => new NoPrioritizer
    case "pps" => new PpsPrioritizer(opt.toInt)
    case "bf" => new BFPrioritizer(opt, 1)
  }
}