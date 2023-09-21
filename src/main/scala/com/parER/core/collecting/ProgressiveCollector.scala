package com.parER.core.collecting

import java.io.{BufferedWriter, File, FileWriter}

import com.parER.core.Config
import com.parER.datastructure.Comparison
import org.scify.jedai.datamodel.IdDuplicates
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import scala.collection.mutable.ListBuffer

class ProgressiveCollector(t0: Long, t1: Long, dp: AbstractDuplicatePropagation, printAll: Boolean = true) {
  import scala.jdk.CollectionConverters._

  var ec = 0.0
  var em = 0.0
  var filling = true
  val duplicates = dp.getDuplicates.asScala
  val buffer = new ListBuffer[String]()
  val nworkers = Config.workers
  val memoryused= Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
  def execute(comparisons: List[Comparison]) = {
    //println("new block ")

    for (cmp <- comparisons) {
      executeCmp(cmp)
    }
  }

  def executeCmp(cmp: Comparison) = {
    //TODO distinguish dirty-cc
    if ( Config.ccer && duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2))) {
      update(cmp.e1, cmp.e2)
    }
    if ( !Config.ccer ) {
      val (a, b) = (duplicates.contains(new IdDuplicates(cmp.e1, cmp.e2)), duplicates.contains(new IdDuplicates(cmp.e2, cmp.e1)))
      if (a || b) {
        (a, b) match {
          case (true, _) => update(cmp.e1, cmp.e2)
          case (_, true) => update(cmp.e2, cmp.e1)
        }
//      if (cmp.blockingKey>7) {
//        println("dup ", cmp.e2, " ", cmp.e1, "blocking  size ",cmp.blockSize, " similaridade ", cmp.sim,  " key ", cmp.blockingKey," prefix ", cmp.blockingThreshold ," --" , em , " -- ", ec)// cmp.e2Model.getItemsFrequency.keySet(), " ", ec, cmp.e1Model.getItemsFrequency.keySet())
//        println(" cmp.e ", cmp.e1Model.getItemsFrequency.keySet())
//        println(" cmp.e ", cmp.e2Model.getItemsFrequency.keySet())
//      }
      }//else
        //println("nao dup ", cmp.e2, " ", cmp.e1, "blocking  size ",cmp.blockSize, " similaridade ", cmp.sim,  " key ", cmp.blockingKey," prefix ", cmp.blockingThreshold ," --" , em , " -- ", ec)// cmp.e2Model.getItemsFrequency.keySet(), " ", ec, cmp.e1Model.getItemsFrequency.keySet())
        //println()

    }
    if (printAll)
      print()
    ec += 1
  }

  def update(e1: Int, e2: Int) = {
    if (!dp.isSuperfluous(e1, e2))
      em += 1
  }

  def getPC() = {
    val pc = em / duplicates.size.toDouble
    pc
  }

  def getPQ() = {
    val pq = em / ec
    pq
  }

  def getCardinaliy() = {
    em
  }

  def print() = {
    if (ec % duplicates.size == 0) {
      val ecX = ec / duplicates.size.toDouble
      val rec = em / duplicates.size.toDouble
      val t = System.currentTimeMillis()
      val dt0 = t - t0
      val dt1 = t - t1
      val afterUsedMem = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
      val actualMemUsed = (afterUsedMem - memoryused)/ (1024 * 1024)
     // println("memory used is " + actualMemUsed / (1024 * 1024) + " mb")
      val s = s"--${ecX}\t${nworkers}\t${dt0}\t${dt1}\t${rec}\t${em}\t${duplicates.size}\t${if (Config.filling) 1 else 0} \t ${ec} \t ${actualMemUsed}"
      buffer.addOne(s)
      println(s)
    }
  }

  def printLast() = {
    val ecX = ec / duplicates.size.toDouble
    val rec = em / duplicates.size.toDouble
    val pq = em / ec
    val t = System.currentTimeMillis()
    val dt0 = t - t0
    val dt1 = t - t1
    val s = s"${ecX}\t${nworkers}\t${dt0}\t${dt1}\t${rec}\t${em}\t${duplicates.size}\t${if (Config.filling) 1 else 0}\t${ec}\t${pq}"
    buffer.addOne(s)
    println(s)
  }

  def writeFile(filename: String): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- buffer) {
      bw.write(line)
      bw.newLine()
    }
    bw.close()
  }
}
