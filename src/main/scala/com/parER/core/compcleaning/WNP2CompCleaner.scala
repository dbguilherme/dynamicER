package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances._
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

class WNP2CompCleaner(dp: AbstractDuplicatePropagation, id:Int) extends HSCompCleaner {

    protected val duplicates=dp.getDuplicates
    private var Tp: Int = 0;
    private var numberSamples: Int = 0;
    private var numberSamplesPos: Int = 0;
    private var Tn: Int = 0;
    private var Fp: Int = 0;
    private var Fn: Int = 0;
    var slopList = ArrayBuffer[Double]()


    private var TpO: Int = 0;
    private var TnO: Int = 0;
    private var FpO: Int = 0;
    private var FnO: Int = 0;
    private var record: Map[String, ListBuffer[Int]] = Map()
    private var totalSize : Int=0;
    private val memory: Int=0;
    private val times: Int=0;
    override def getRecall(): Double = {
        0.0
    }

    override def getPrecision(): Double = {
      0.0
    }

//
   override def getTotalSize():Int = {
    totalSize
  }

  /**
   * Determines the angle of a straight line drawn between point one and two. The number returned, which is a double in degrees, tells us how much we have to rotate a horizontal line clockwise for it to match the line between the two points.
   * If you prefer to deal with angles using radians instead of degrees, just change the last line to: "return Math.atan2(yDiff, xDiff);"
   */
  def GetAngleOfLineBetweenTwoPoints()= {
    var lastId = record("x").length - 1

    var teste=0.0
    if (lastId > 1) {
      var xDiff = (record("x")(lastId ) - record("x")(lastId-1))
      var yDiff = (record("y")(lastId ) - record("y")(lastId-1))
      record.getOrElseUpdate("slop", ListBuffer()) +=  yDiff/ xDiff


//      if (teste<0)
//        teste=teste+360
     // println("Math.atan2(yDiff, xDiff) " , yDiff/ xDiff ,  "angle is ", teste , " dif ", yDiff, " ", xDiff)
    }else
      record.getOrElseUpdate("slop", ListBuffer()) +=  0


  }


  override def execute(comparisons: List[Comparison]) = {
    if (comparisons.size == 0)
      comparisons
    else {
      var cmps = removeRedundantComparisons(comparisons)
      //   val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
      //   cmps = cmps.filter(_.sim >= w)

      record.getOrElseUpdate("y", ListBuffer()) += totalSize
      record.getOrElseUpdate("x", ListBuffer()) += cmps.head.e2


      totalSize += cmps.size
      if (cmps.head.e2 % 100 == 0) {

        GetAngleOfLineBetweenTwoPoints()
        (fpeaks(record("x").toArray, record("slop").toArray))
//        slopList+= (slop())
        //        GetAngleOfLineBetweenTwoPoints()

      }
      //smoothedZScore(slopList.toSeq,1,1,1)
      //print(" slopList ", slopList)
      //find the max element of a slopList


//      for (i <- slopList.indices) {
//        if (slopList(i) > memory) {
//          memory = slopList(i)
//          if (times > 2)
//            println("pos ", record("pos"))
//          else
//            times = 0
//        }
//
//      }


      cmps
    }
  }



  def fpeaks(x: Array[Int], y: Array[Int]): List[Double] = {
    // Find peaks
    val peaks = find_peaks(y, height = 100, threshold = 10, distance = 5, )
    //val height = peaks[0] // list of the heights of the peaks
    val peak_pos = ListBuffer[Double]()
//    for (i <- peaks) {
//      peak_pos += x(i)
//    }
    if (peaks.size>0) {
      println("found a peak pos",  peak_pos, "  ", peaks)
    }
    // val peak_pos = x(peaks) // list of the peaks positions

    // println("peak_pos", peak_pos)

    // Plotting
    (peak_pos.ls)
  }

  def find_peaks(y: Array[Int], height: Double, threshold: Double, distance: Int,lastpeak : Int): ListBuffer[Int] = {
    val peakPositions = new ListBuffer[Int]()
    val peakHeights = new ListBuffer[Double]()

    var i = 1
    while (i < y.length - 1) {
      if (y(i) > height && y(i) - y(i - 1) > threshold && y(i) - y(i + 1) > threshold) {
        peakPositions += i
        peakHeights += y(i)
        i += distance
      } else {
        i += 1
      }
    }
    print("peak ", peakPositions, " ", peakHeights)
//    val peaks = (peakPositions.toArray, Map("peak_heights" -> peakHeights.toArray))
    (peakPositions)
  }
  /**
   * Smoothed zero-score alogrithm shamelessly copied from https://stackoverflow.com/a/22640362/6029703
   * Uses a rolling mean and a rolling deviation (separate) to identify peaks in a vector
   *
   * @param y         - The input vector to analyze
   * @param lag       - The lag of the moving window (i.e. how big the window is)
   * @param threshold - The z-score at which the algorithm signals (i.e. how many standard deviations away from the moving mean a peak (or signal) is)
   * @param influence - The influence (between 0 and 1) of new signals on the mean and standard deviation (how much a peak (or signal) should affect other values near it)
   * @return - The calculated averages (avgFilter) and deviations (stdFilter), and the signals (signals)
   */
//  private def smoothedZScore(y: Seq[Double], lag: Int, threshold: Double, influence: Double): Seq[Int] = {
//    val stats = new SummaryStatistics()
//
//    // the results (peaks, 1 or -1) of our algorithm
//    val signals = mutable.ArrayBuffer.fill(y.length)(0)
//
//    // filter out the signals (peaks) from our original list (using influence arg)
//    val filteredY = y.to[mutable.ArrayBuffer]
//
//    // the current average of the rolling window
//    val avgFilter = mutable.ArrayBuffer.fill(y.length)(0d)
//
//    // the current standard deviation of the rolling window
//    val stdFilter = mutable.ArrayBuffer.fill(y.length)(0d)
//
//    // init avgFilter and stdFilter
//    y.take(lag).foreach(s => stats.addValue(s))
//
//    avgFilter(lag - 1) = stats.getMean
//    stdFilter(lag - 1) = Math.sqrt(stats.getPopulationVariance) // getStandardDeviation() uses sample variance (not what we want)
//
//    // loop input starting at end of rolling window
//    y.zipWithIndex.slice(lag, y.length - 1).foreach {
//      case (s: Double, i: Int) =>
//        // if the distance between the current value and average is enough standard deviations (threshold) away
//        if (Math.abs(s - avgFilter(i - 1)) > threshold * stdFilter(i - 1)) {
//          // this is a signal (i.e. peak), determine if it is a positive or negative signal
//          signals(i) = if (s > avgFilter(i - 1)) 1 else -1
//          // filter this signal out using influence
//          filteredY(i) = (influence * s) + ((1 - influence) * filteredY(i - 1))
//        } else {
//          // ensure this signal remains a zero
//          signals(i) = 0
//          // ensure this value is not filtered
//          filteredY(i) = s
//        }
//
//        // update rolling average and deviation
//        stats.clear()
//        filteredY.slice(i - lag, i).foreach(s => stats.addValue(s))
//        avgFilter(i) = stats.getMean
//        stdFilter(i) = Math.sqrt(stats.getPopulationVariance) // getStandardDeviation() uses sample variance (not what we want)
//    }
//
//    println(y.length)
//    println(signals.length)
//    println(signals)
//
//    signals.zipWithIndex.foreach {
//      case (x: Int, idx: Int) =>
//        if (x == 1) {
//          println(idx + " " + y(idx))
//        }
//    }
//
//    val data =
//      y.zipWithIndex.map { case (s: Double, i: Int) => Map("x" -> i, "y" -> s, "name" -> "y", "row" -> "data") } ++
//        avgFilter.zipWithIndex.map { case (s: Double, i: Int) => Map("x" -> i, "y" -> s, "name" -> "avgFilter", "row" -> "data") } ++
//        avgFilter.zipWithIndex.map { case (s: Double, i: Int) => Map("x" -> i, "y" -> (s - threshold * stdFilter(i)), "name" -> "lower", "row" -> "data") } ++
//        avgFilter.zipWithIndex.map { case (s: Double, i: Int) => Map("x" -> i, "y" -> (s + threshold * stdFilter(i)), "name" -> "upper", "row" -> "data") } ++
//        signals.zipWithIndex.map { case (s: Int, i: Int) => Map("x" -> i, "y" -> s, "name" -> "signal", "row" -> "signal") }
//
//    Vegas("Smoothed Z")
//      .withData(data)
//      .mark(Line)
//      .encodeX("x", Quant)
//      .encodeY("y", Quant)
//      .encodeColor(
//        field = "name",
//        dataType = Nominal
//      )
//      .encodeRow("row", Ordinal)
//      .show
//
//    return signals
//  }


  def slop (): Double = {
    var lastId=record("x").length-1
    var slop: Double=0.0
    if (lastId>100)
      slop= (record("y")(lastId)-record("y")(lastId-100))/(record("x")(lastId)-record("x")(lastId-100))

   // println("slop ---", slop)
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
    var temp = " "+rho.toString+ " "
   // print (temp)
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
