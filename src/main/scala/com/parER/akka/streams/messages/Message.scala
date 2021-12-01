package com.parER.akka.streams.messages

import com.parER.datastructure.Comparison
import org.scify.jedai.textmodels.TokenNGrams

abstract class Message

case class Update(id: Int, model: TokenNGrams) extends Message

case class UpdateSeq(updateSeq: Seq[Update]) extends Message

case class Comparisons(comparisons: List[Comparison]) extends Message

case class ComparisonsSeq(comparisonSeq: Seq[Comparisons]) extends Message

case class BlockTuple(id: Int, model: TokenNGrams, blocks: List[List[Int]]) extends Message

case class BlockTupleSeq(blockTupleSeq: Seq[BlockTuple]) extends Message

case class AggregatorMessage(id: String, e1: Int, e1Model: TokenNGrams, blocks: List[List[Int]])