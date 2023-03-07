package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances
import org.scify.jedai.textmodels.TokenNGrams
import moa.classifiers.trees.HoeffdingTree
import moa.classifiers.Classifier
import moa.core.TimingUtils
import moa.streams.generators.RandomRBFGenerator
import com.yahoo.labs.samoa.instances.{Attribute, DenseInstance, Instance, Instances, InstancesHeader}
import org.scify.jedai.datamodel.IdDuplicates
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import java.io.IOException
import java.util
import java.util.{HashSet, Random, Set}


class WNP2CompCleaner(dp: AbstractDuplicatePropagation) extends HSCompCleaner {



   val duplicates=dp.getDuplicates

    private var Tp: Int = 0;
    private var numberSamples: Int = 0;
    private var Tn: Int = 0;
    private var Fp: Int = 0;
    private var Fn: Int = 0;


    private var numberSamplesCorrectOriginal: Int = 0;
    private var numberSamplesInCorrectOriginal: Int = 0;

    private var inst:Instance = createInstance()
    private var learner:HoeffdingTree = createClassifier()
    private def createClassifier() : HoeffdingTree = {
      learner = new HoeffdingTree();
      val stream = new RandomRBFGenerator();
      stream.prepareForUse();

      learner.setModelContext(stream.getHeader());
      learner.prepareForUse();
      learner
    }

    private def createInstance(): DenseInstance = {
      // generates the name of the features which is called as InstanceHeader
      //val attributes = new Nothing
      var attributes = new util.ArrayList[Attribute]()
      for (i <- 0 until 3) {
        attributes.add(new Attribute("feature_" + i))
      }
      // create instance header with generated feature name
      val streamHeader = new InstancesHeader(new Instances("Mustafa Çelik Instance", attributes, 10));
      streamHeader.setClassIndex(2)
      // generates random data
      val data = new Array[Double](3)
      val random = new Random()
      //    for (i <- 0 until 2) {
      //      data( i ) = random.nextDouble
      //    }
      //    data(2)=1
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
      val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
     // cmps = cmps.filter(_.sim >= w)


//
//





      val preciseCPUTiming = TimingUtils.enablePreciseTiming();
      val evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
      //while (stream.hasMoreInstances() && numberSamples < 10) {
      for (cmp <- cmps){

        val commonKeysA = cmp.e1Model.getItemsFrequency.size()
//        commonKeys.retainAll(cmp.e2Model.getItemsFrequency.keySet)

        val commonKeysB = cmp.e2Model.getItemsFrequency.size()
//        println(commonKeysA )
//        println(commonKeysB )
//        println(cmp.e2Model.getItemsFrequency.size())

        val commonKeys = new util.HashSet[String](cmp.e1Model.getItemsFrequency.keySet)
        commonKeys.retainAll(cmp.e2Model.getItemsFrequency.keySet)

        val numerator:Double = commonKeys.size
        val denominator:Double = cmp.e1Model.getItemsFrequency.size + cmp.e2Model.getItemsFrequency.size - numerator
        val sim=numerator / denominator
        println("siml =",sim )


        var e= cmp.e1Model.getItemsFrequency//getSimilarity(cmp.e2Model)
        //println("---",e)
        inst.setValue(0,0)
        inst.setValue(1,sim)
        val (a, b) = (duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2)), duplicates.contains(new IdDuplicates(cmp.e2, cmp.e1)))
        if (a || b) {
               inst.setClassValue(2,1)
              if(cmp.sim>=w )
                numberSamplesCorrectOriginal+=1
              else
                numberSamplesInCorrectOriginal+=1


        } else {
          inst.setClassValue(2,0)
          if (cmp.sim >= w)
            numberSamplesInCorrectOriginal += 1
          else
            numberSamplesCorrectOriginal += 1
        }

        var label=learner.correctlyClassifies(inst)
        println(inst.toString, " label ",label , a , b)
        learner.getVotesForInstance(inst).foreach(println)
    if(label)
      Tp+=1
    else
      Tn+=1


        numberSamples += 1;
       // if (numberSamples<10)
          learner.trainOnInstance(inst);
        println("instancia é " , inst.toString)
      }
      //val accuracy = 100.0 * numberSamplesCorrect/ numberSamples;

      println(" instances processed with "  + "% accuracy ", "tp", Tp , "  fn ",Fn, " Fp  ",Fp, " Tn ", Tn);
      println(" instances processed with "  + "% accuracy ", "true posi", numberSamplesCorrectOriginal , "  numberSamplesInCorrect ",numberSamplesInCorrectOriginal);

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
