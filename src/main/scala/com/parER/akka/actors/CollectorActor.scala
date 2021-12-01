package com.parER.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.parER.datastructure.Comparison

import scala.collection.mutable.ListBuffer

object CollectorActor {

  final case class Comparisons(comparisons: List[Comparison])

  val comparisons = new ListBuffer[Comparison]()

  def apply(): Behavior[Comparisons] = Behaviors.receive { (context, message) =>
    comparisons ++= message.comparisons
    apply()
  }

}
