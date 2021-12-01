package com.parER.akka.streams

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.parER.core.collecting.ProgressiveCollector
import com.parER.datastructure.Comparison
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation

import scala.concurrent.{Future, Promise}

class ProgressiveCollectorSink(t0: Long, t1:Long, dp: AbstractDuplicatePropagation, print: Boolean = true) extends GraphStageWithMaterializedValue[SinkShape[List[Comparison]], Future[Long]] {
  val in: Inlet[List[Comparison]] = Inlet("CollectorSink")
  override val shape: SinkShape[List[Comparison]] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val p: Promise[Long] = Promise()
    val logic = new GraphStageLogic(shape) {

      val proColl = new ProgressiveCollector(t0, t1, dp, print)
      var counter = 0L;

      // This requests one element at the Sink startup.
      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val comparisons = grab(in)
          counter += comparisons.size
          proColl.execute(comparisons)
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          val result = counter
          proColl.printLast()
          p.trySuccess(result)
          completeStage()
        }
      })
    }
    (logic, p.future)
  }
}
