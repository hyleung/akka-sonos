package com.example

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.example.actors.ApiBridge
import com.example.protocol.ApiProtocol.{ZonesResponse, ZonesRequest}
import com.typesafe.scalalogging.LazyLogging
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object ApplicationMain extends App with LazyLogging {
  implicit val system = ActorSystem("SonosApiBridge")
  val supervisor = system.actorOf(Props[ApiBridge])
  val startTime = System.currentTimeMillis()

  implicit val timeout = Timeout(5 seconds)
  supervisor ? ZonesRequest() onSuccess {
    case msg => msg match {
      case ZonesResponse(s) =>
        println(s)
        system.terminate()
    }
  }
}