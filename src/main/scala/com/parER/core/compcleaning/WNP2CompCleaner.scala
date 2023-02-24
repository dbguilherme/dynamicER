package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import org.apache.spark
import org.apache.spark.ml.classification.LogisticRegression
import org.scify.jedai.textmodels.TokenNGrams
import org.apache.spark.*;

class WNP2CompCleaner extends HSCompCleaner {

  val teste=0


  def run() = {
    // Load training data
    val training = spark.read.format("libsvm").load("data/mllib/sample_libsvm_data.txt")

    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)

    // Fit the model
    val lrModel = lr.fit(training)

    // Print the coefficients and intercept for logistic regression
    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")

    // We can also use the multinomial family for binary classification
    val mlr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)
      .setFamily("multinomial")

    val mlrModel = mlr.fit(training)

    // Print the coefficients and intercepts for logistic regression with multinomial family
    println(s"Multinomial coefficients: ${mlrModel.coefficientMatrix}")
    println(s"Multinomial intercepts: ${mlrModel.interceptVector}")

    override def execute(comparisons: List[Comparison]) = {
      if (comparisons.size == 0)
        comparisons
      else {
        removeRedundantComparisons(comparisons)
      }
    }
  }

  override def execute(comparisons: List[Comparison]) = {

    if (comparisons.size == 0)
      comparisons
    else {
      var cmps = removeRedundantComparisons(comparisons)
      val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
      cmps = cmps.filter(_.sim >= w)
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
