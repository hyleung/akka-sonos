package com.example

import akka.actor.ActorSystem
import akka.io.{IO, Udp}
import com.example.protocol.DiscoveryProtocol
import DiscoveryProtocol.StartDiscovery
import com.example.actors.{SonosApiActor, DiscoveryActor}
import com.example.protocol.SonosProtocol.ZoneQuery
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
object ApplicationMain extends App with LazyLogging {
  implicit val system = ActorSystem("MyActorSystem")
  val MULTICAST_ADDR = "239.255.255.250"
  val MULTICAST_PORT = 0
  val udp = IO(Udp)
  //val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR,MULTICAST_PORT,udp))
  val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR, "en0", MULTICAST_PORT, udp))
  val startTime = System.currentTimeMillis()
  discovery ! StartDiscovery()

  logger.info(s"Discovery completed in ${System.currentTimeMillis() - startTime} ms")

  val zones = system.actorOf(SonosApiActor.props("http://192.168.1.83:1400"))
  zones ! ZoneQuery()

  Await.result(system.whenTerminated,5 seconds)
}