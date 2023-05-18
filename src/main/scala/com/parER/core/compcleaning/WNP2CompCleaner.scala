package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances._
import moa.classifiers.trees.HoeffdingTree
import org.scify.jedai.textmodels.TokenNGrams
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import java.util


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

    private var inst:Instance = createInstance()
    private var learner: HoeffdingTree = createClassifier()
    private def createClassifier() = {
      learner = new HoeffdingTree()

      //val stream = new RandomRBFGenerator();//new NaiveBayes();
      //stream.prepareForUse();

     // learner.setModelContext(stream.getHeader());
      learner.prepareForUse();
      learner
    }
    override def getRecall(): Double = {
        0.0
    }

    override def getPrecision(): Double = {
      0.0
    }

    private def createInstance(): DenseInstance = {
      // generates the name of the features which is called as InstanceHeader

      var attributes = new util.ArrayList[Attribute]()
      for (i <- 0 until 6) {
        attributes.add(new Attribute("feature_" + i))
      }
      // create instance header with generated feature name
      val
      streamHeader = new InstancesHeader(new Instances("Instance", attributes, 10));
      streamHeader.setClassIndex(5)
      // generates random data
      val data = new Array[Double](6)

      // creates an instance and assigns the data
      val instance = new DenseInstance(1.0, data)
      // assigns the instanceHeader(feature name)
      instance.setDataset(streamHeader)

      instance
    }

  override def execute(comparisons: List[Comparison]) = {
    if (comparisons.size == 0)
      comparisons
    else {
      var cmps = removeRedundantComparisons(comparisons)
     // val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
    //  cmps = cmps.filter(_.sim >= w)


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
