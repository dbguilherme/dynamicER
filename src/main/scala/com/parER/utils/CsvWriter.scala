package com.parER.utils

import java.io.{BufferedWriter, File, FileWriter}

import scala.collection.mutable.ListBuffer

class CsvWriter(header: String) {

  var buffer = new ListBuffer[String]()

  def newLine(line: List[String]) = {
    var sb = new StringBuilder
    for (l <- line)
      sb ++= ( l + "," )
    CsvWriter.lastChar(sb)
    buffer.append(sb.result)
  }

  def writeFile(filename: String, append: Boolean): Unit = {
    var flag=false
    val file=new File(filename)
    if (!file.exists()){
      flag=true
    }
    var bw = new BufferedWriter(new FileWriter(file, append))
    if (flag){
      bw.write(header)
      bw.newLine()
    }
    if (!append) {

    }
    for (line <- buffer) {
      bw.write(line)
      bw.newLine()
    }
    bw.close()
  }
}

object CsvWriter {
  def apply(header: List[String]) = {
    var sb = new StringBuilder
    for (h <- header)
      sb ++= ( h + "," )
    lastChar(sb)
    new CsvWriter(sb.result)
  }

  def lastChar(sb: StringBuilder) = {
    sb.deleteCharAt(sb.length() - 1)
  }
}
