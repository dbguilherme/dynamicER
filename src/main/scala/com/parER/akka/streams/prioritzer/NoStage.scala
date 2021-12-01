package com.parER.akka.streams.prioritzer

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.parER.datastructure.Comparison

class NoStage(name: String, opt: Long = 0) extends GraphStage[FlowShape[List[Comparison], List[Comparison]]] {
  val in = Inlet[List[Comparison]]("NoStage.in")
  val out = Outlet[List[Comparison]]("NoStage.out")

  override val shape = FlowShape.of(in, out)

  //println(s"KMax: ${opt.toInt}")

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          push(out, grab(in))
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}
