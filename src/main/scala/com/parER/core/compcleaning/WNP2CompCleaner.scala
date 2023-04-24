package com.parER.core.compcleaning

import com.parER.datastructure.Comparison
import com.yahoo.labs.samoa.instances
import org.scify.jedai.textmodels.TokenNGrams
import moa.classifiers.trees.{HoeffdingOptionTree, HoeffdingTree}
import moa.classifiers.{AbstractClassifier, Classifier}
import moa.core.{TimingUtils, Utils}
import moa.streams.generators.RandomRBFGenerator
import com.yahoo.labs.samoa.instances.{Attribute, DenseInstance, Instance, Instances, InstancesHeader}
import moa.classifiers.bayes.NaiveBayes
import org.scify.jedai.datamodel.IdDuplicates
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import java.io.IOException
import java.util
import java.util.{HashSet, Random, Set}


class WNP2CompCleaner(dp: AbstractDuplicatePropagation) extends HSCompCleaner {



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
         val recall= (Tp:Double) / (Tp + Fn)//, " precision ", (Tp:Double) / (Tp + Fp))
         recall
      }



    override def getPrecision(): Double = {
      val
      precision = (Tp:Double) / (Tp + Fp)
      precision
    }

    private def createInstance(): DenseInstance = {
      // generates the name of the features which is called as InstanceHeader
      //val
      // attributes = new Nothing
      var attributes = new util.ArrayList[Attribute]()
      for (i <- 0 until 6) {
        attributes.add(new Attribute("feature_" + i))
      }
      // create instance header with generated feature name
      val
      streamHeader = new InstancesHeader(new Instances("Mustafa Çelik Instance", attributes, 10));
      streamHeader.setClassIndex(5)
      // generates random data
      val data = new Array[Double](6)
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
      cmps = cmps.filter(_.sim >= w)


//
//





//      val preciseCPUTiming = TimingUtils.enablePreciseTiming();
//      val evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
//      //while (stream.hasMoreInstances() && numberSamples < 10) {
//      for (cmp <- cmps){
//
//        val commonKeysA = cmp.e1Model.getItemsFrequency.size()
////        commonKeys.retainAll(cmp.e2Model.getItemsFrequency.keySet)
//
//        val commonKeysB = cmp.e2Model.getItemsFrequency.size()
////        println(commonKeysA )
////        println(commonKeysB )
////        println(cmp.e2Model.getItemsFrequency.size())
//
//        //val commonKeys = new util.HashSet[String](cmp.e1Model.getItemsFrequency.keySet)
//       // commonKeys.retainAll(cmp.e2Model.getItemsFrequency.keySet)
//
//        val numerator:Double = cmp.sim
//        val denominator:Double = cmp.e1Model.getItemsFrequency.size + cmp.e2Model.getItemsFrequency.size - numerator
//        val sim=numerator / denominator
//        //println("siml =",sim )
//
//
//        var e= cmp.e1Model.getItemsFrequency//getSimilarity(cmp.e2Model)
//        //println("---",e)
//        inst.setValue(0,(1.0)/cmp.blockSize)
//        inst.setValue(1,sim)
//        inst.setValue(2,cmp.e1Model.getItemsFrequency.size)
//        inst.setValue(3,cmp.e2Model.getItemsFrequency.size)
//        inst.setValue(4,numerator)
//
//        //println(cmp.blockSize)
//
//        val (a, b) = (duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2)), duplicates.contains(new IdDuplicates(cmp.e2, cmp.e1)))
//        if (a || b) {
//               inst.setClassValue(2,1)
//              if(cmp.sim>=w )
//                TpO+=1
//              else
//                FnO+=1
//
//
//        } else {
//          inst.setClassValue(2,0)
//          if (cmp.sim >= w)
//            FpO += 1
//          else
//            TnO += 1
//        }
//
//        var label=learner.correctlyClassifies(inst)
//        //println(inst.toString, " label ",label , a , b)
//        var prediction = learner.getPredictionForInstance(inst)
//        var vet =learner.getVotesForInstance(inst)
//        //  vet foreach println
//        //println()
//      if(numberSamplesPos>10) {
//        if (a || b) {
//          if (Utils.maxIndex(learner.getVotesForInstance(inst)) == 1)
//            Tp += 1
//          else
//            Fn += 1
//        }
//        else {
//          if (Utils.maxIndex(learner.getVotesForInstance(inst)) == 1)
//            Fp += 1
//          else
//            Tn += 1
//        }
//      }
//
//        numberSamples += 1;
//        if (inst.classValue()==1)
//          numberSamplesPos += 1;
//
//
//       // if (numberSamplesPos < (duplicates.size()/2))
//        if (Tp<5)
//          learner.trainOnInstance(inst);
//       // println("instancia é " , inst.toString)
//      }
//      //val accuracy = 100.0 * numberSamplesCorrect/ numberSamples;
//
//
//      if (numberSamples>8750) {
//        println("Proposta instances processed with "  + "% accuracy ", "tp", Tp , "  fn ",Fn, " Fp  ",Fp, " Tn ", Tn);
//        println("recall ", (Tp:Double) / (Tp + Fn), " precision ", (Tp:Double) / (Tp + Fp))
//        println("Original instances processed with " + "% accuracy ", "tp", TpO, "  fn ", FnO, " Fp  ", FpO, " Tn ", TnO);
//        println("recall ", (TpO:Double) / (TpO + FnO), " precision ", (TpO:Double) / (TpO + FpO))
//        println()
//      }
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
