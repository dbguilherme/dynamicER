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
import java.util.Random


class WNP2CompCleaner(dp: AbstractDuplicatePropagation) extends HSCompCleaner {



   val duplicates=dp.getDuplicates

    private var numberSamplesCorrect: Int = 0;
    private var numberSamples: Int = 0;
    private var numberSamplesInCorrect: Int = 0;


    private var numberSamplesCorrectOriginal: Int = 0;
    private var numberSamplesInCorrectOriginal: Int = 0;

    private var inst:Instance = createInstance()
    private def createInstance(): DenseInstance = {
      // generates the name of the features which is called as InstanceHeader
      //val attributes = new Nothing
      var attributes = new util.ArrayList[Attribute]()
      for (i <- 0 until 1) {
        attributes.add(new Attribute("feature_" + i))
      }
      // create instance header with generated feature name
      val streamHeader = new InstancesHeader(new Instances("Mustafa Çelik Instance", attributes, 2));
      streamHeader.setClassIndex(2)
      // generates random data
      val data = new Array[Double](3)
      val random = new Random()
      //    for (i <- 0 until 2) {
      //      data( i ) = random.nextDouble
      //    }
      //    data(2)=1
      // creates an instance and assigns the data
      val instancia = new DenseInstance(1.0, data)
      // assigns the instanceHeader(feature name)
      instancia.setDataset(streamHeader)

      instancia
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
      val learner = new HoeffdingTree();
      val stream = new RandomRBFGenerator();

      stream.prepareForUse();

      learner.setModelContext(stream.getHeader());
      learner.prepareForUse();



      val preciseCPUTiming = TimingUtils.enablePreciseTiming();
      val evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
      //while (stream.hasMoreInstances() && numberSamples < 10) {
      for (cmp <- cmps){



        inst.setValue(0,cmp.sim)
        inst.setValue(1,w)
        val (a, b) = (duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2)), duplicates.contains(new IdDuplicates(cmp.e2, cmp.e1)))
        if (a || b) {
               inst.setClassValue(2,0.0)
              if(cmp.sim>=w )
                numberSamplesCorrectOriginal+=1
              else
                numberSamplesInCorrectOriginal+=1


        } else {
          inst.setClassValue(2,0.0)
          if (cmp.sim >= w)
            numberSamplesInCorrectOriginal += 1
          else
            numberSamplesCorrectOriginal += 1
        }

        var label=learner.correctlyClassifies(inst)
        println(inst.toString, " label ",label , a , b)

         if (a || b) {
           if (label)
             numberSamplesCorrect += 1;
           else
             numberSamplesInCorrect += 1
         }
         else{
             if (label) {
               numberSamplesInCorrect += 1;
             } else {
               numberSamplesCorrect += 1
             };
           }


        numberSamples += 1;
       // if (numberSamples<10)
          learner.trainOnInstance(inst);
      }
      val accuracy = 100.0 * numberSamplesCorrect/ numberSamples;

      println(" instances processed with " + accuracy + "% accuracy ", "true posi", numberSamplesCorrect , "  numberSamplesInCorrect ",numberSamplesInCorrect);
      println(" instances processed with " + accuracy + "% accuracy ", "true posi", numberSamplesCorrectOriginal , "  numberSamplesInCorrect ",numberSamplesInCorrectOriginal);

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
