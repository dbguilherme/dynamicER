package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances._
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import scala.collection.mutable.{ListBuffer, Map}



class WNP2CompCleaner(dp: AbstractDuplicatePropagation, id:Int) extends HSCompCleaner {

    protected val duplicates=dp.getDuplicates
    private var Tp: Int = 0;
    private var numberSamples: Int = 0;
    private var numberSamplesPos: Int = 0;
    private var Tn: Int = 0;
    private var Fp: Int = 0;
    private var Fn: Int = 0;


    private var TpO: Int = 0;
    private var TnO: Int = 0;
    private var FpO: Int = 0;
    private var FnO: Int = 0;
    private var record: Map[String, ListBuffer[Int]] = Map()

    private var totalSize : Int=0;

    override def getRecall(): Double = {
        0.0
    }

    override def getPrecision(): Double = {
      0.0
    }

   override def getTotalSize():Int = {
    totalSize
  }


  override def execute(comparisons: List[Comparison]) = {
    if (comparisons.size == 0)
      comparisons
    else {
      var cmps = removeRedundantComparisons(comparisons)
   //   val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
   //   cmps = cmps.filter(_.sim >= w)

      //if (cmps.size>10)
      //  println(id, "size is ", cmps.size)
      totalSize+=cmps.size

      record.getOrElseUpdate("pos",ListBuffer())+=totalSize
      record.getOrElseUpdate("x",ListBuffer())+=cmps.head.e2
//      println("pos ", record("pos"))
//      println("x " , record("x"))
      knee()
      slop()
      cmps
    }
  }

  def slop ():Double = {
    var lastId=record("x").length-1
    var slop=0
    if (lastId>2)
      slop= (record("pos")(lastId)-record("pos")(lastId-1))/(record("x")(lastId)-record("x")(lastId-1))

    println("slop ", slop)


  slop
  }

  def knee(): Boolean = {
    val y = record("pos").last
    val s = record("x").last
    var ratio = 0.0
    try {
      ratio = s / math.sqrt(math.pow(y, 2) + math.pow(s, 2))
    } catch {
      case _: ArithmeticException =>
        println("Error: Division by zero.")
        ratio = 0
    }

    var per_best = -1.0
    var best = 0

    for (i <- record("x").indices.reverse) {
      val per = (record("pos")(i) - record("x")(i) * y / s) * ratio

      if (per > per_best) {
        best = i
        per_best = per
      }
    }
    var rho = 0.0
    try {
      rho = (s - record("x")(best)) * record("pos")(best) / record("x")(best) / (1 + y - record("pos")(best))
    } catch {
      case _: ArithmeticException =>
        println("Error: Division by zero.")
        rho = 0
    }
    val thres = 6
    print ("rho value ", rho)
    if (rho >= thres) true else false
  }

  override def execute(id: Int, model: TokenNGrams, ids: List[Int]): (Int, TokenNGrams, List[Int]) = {
    if (ids.size == 0) {
      (id, model, ids)
    } else {
      var hm = removeRedundantIntegers(ids)
      val w = hm.values.foldLeft(0.0)( (v, c) => v + c.toDouble) / hm.values.size
      (id, model, hm.filter(_._2.toDouble >= w).keys.toList)
    }
  }

}
