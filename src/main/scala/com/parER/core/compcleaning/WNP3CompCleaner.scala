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
import moa.classifiers.active.ALUncertainty

class WNP3CompCleaner(dp: AbstractDuplicatePropagation) extends HSCompCleaner {



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
  private var learner: AbstractClassifier = createClassifier()
  private def createClassifier() = {
    //learner = new HoeffdingTree()
    //var learner = new HoeffdingTree;//new NaiveBayes();
    var learner = new ALUncertainty;
    learner.budgetOption.setValue(0.3);
    learner.activeLearningStrategyOption.setValue(0);
    //stream.prepareForUse();

//    learner.removePoorAttsOption.setValue(true);
//    learner.noPrePruneOption.setValue(true);
//    learner.splitConfidenceOption.setValue(1);
    //learner.deactivateAllLeaves()
    // learner.setModelContext(stream.getHeader());
    learner.prepareForUse();
    learner
  }
  override def getRecall(): Double = {
    val recall= (Tp:Double) / (Tp + Fn)//, " precision ", (Tp:Double) / (Tp + Fp))
    recall
  }

  override def getPrecision(): Double = {
    val precision = (Tp:Double) / (Tp + Fp)
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
      val clean_comparisons = List.newBuilder[Comparison]
      var cmps = removeRedundantComparisons(comparisons)
      val w = cmps.foldLeft(0.0)( (v, c) => v + c.sim).toDouble / cmps.size
      //cmps = cmps.filter(_.sim >= w/3)

      val preciseCPUTiming = TimingUtils.enablePreciseTiming();
      val evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

      for (cmp <- cmps){

        val numerator:Double = cmp.sim
        val denominator:Double = cmp.e1Model.getItemsFrequency.size + cmp.e2Model.getItemsFrequency.size - numerator
        val sim=numerator / denominator


        inst.setValue(0,(1.0)/cmp.blockSize)
        inst.setValue(1,cmp.sim)
        inst.setValue(2,cmp.e1Model.getItemsFrequency.size)
        inst.setValue(3,cmp.e2Model.getItemsFrequency.size)
        inst.setValue(4,sim)

        //println(cmp.blockSize)

        val (a, b) = (duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2)), duplicates.contains(new IdDuplicates(cmp.e2, cmp.e1)))
        if(a || b){
          inst.setClassValue(1.0)
        }else
          inst.setClassValue(0.0)

        if (a || b) {
          if(cmp.sim>=w )
            TpO+=1
          else
            FnO+=1
        } else {
          if (cmp.sim >= w)
            FpO += 1
          else
            TnO += 1
        }

        //var label=learner.correctlyClassifies(inst)

        var votes=Utils.maxIndex(learner.getVotesForInstance(inst))
        if (votes==1)
          clean_comparisons.addOne(cmp)
        //  {


          if (a || b) {
            if (votes == 1) {
              Tp += 1
            }
            else {
              Fn += 1
              //println("instancia ", inst.toString , " ", numberSamplesPos)
            }
          }
          else {
            if (votes==1) {
              Fp += 1
            } else
              Tn += 1
          }

        numberSamples += 1;
        if (votes==1)
          numberSamplesPos += 1;


      //  if (numberSamplesPos<100)
          learner.trainOnInstanceImpl(inst);
      }
      //var res  = cmps.filter(_.filterflag == 0) //note working!!!
      var measurements= learner.getModelMeasurements()

      for (i <- measurements) {
        print("measu ", i.toString , ' ');
      }
      println()



      clean_comparisons.result()

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
