package com.parER.datastructure

import org.scify.jedai.textmodels.TokenNGrams

case class Comparison(val e1: Int, val e1Model: TokenNGrams, val e2: Int, val e2Model: TokenNGrams, var sim : Double = 0.0,var blockingKey : Int=0,  var blockSize:Int=0 ,var filterflag: Int=0) {
  //val counters : Array[Int] = Array(0,0)
}