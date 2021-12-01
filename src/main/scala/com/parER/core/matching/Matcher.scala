package com.parER.core.matching

import com.parER.datastructure.Comparison

trait Matcher {
  def execute(comparisons: List[Comparison]): List[Comparison]
  def execute(comparison: Comparison) : Comparison
  def getName(): String
}

object Matcher {
  def apply(name: String) = name match {
    case "no" => null
    case "js" => new JSMatcher
  }
}
