package com.parER.akka.streams

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.parER.akka.streams.messages._
import com.parER.core.blocking.Blocking
import org.scify.jedai.textmodels.TokenNGrams

class ProcessingSeqTokenBlockerStage(name: String, size1: Int, size2: Int = 0, ro: Double = 0.005, ff: Double = 0.01) extends GraphStage[FlowShape[Seq[(Int, TokenNGrams)], Message]] {
  val in = Inlet[Seq[(Int, TokenNGrams)]]("ProcessingSeqTokenBlockerStage.in")
  val out = Outlet[Message]("ProcessingSeqTokenBlockerStage.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      val tokenBlocker = Blocking.apply(name, size1, size2, ro, ff)
      tokenBlocker.setModelStoring(false)

      setHandler(in, new InHandler {

        override def onPush(): Unit = {

          val items   = grab(in)
          val updates = Seq.newBuilder[Update]
          val bTuples = Seq.newBuilder[BlockTuple]

          for ((i,p) <- items) {
            val t = tokenBlocker.process(i, p)
            if (t._3.size > 0) {
              updates += Update(i,p)
              bTuples += BlockTuple(t._1, t._2, t._3)
            } else {
              updates += Update(i,p)
            }
          }

          if (bTuples.knownSize > 0) {
            val msg = List(UpdateSeq(updates.result()), BlockTupleSeq(bTuples.result()))
            emitMultiple[Message](out, msg)
          } else {
            push(out, UpdateSeq(updates.result()))
          }
        }

      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}