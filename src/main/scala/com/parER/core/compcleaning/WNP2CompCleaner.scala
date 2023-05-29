package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances._
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation


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
      cmps

    }
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
