package com.example

import akka.actor.ActorSystem
import akka.io.{IO, Udp}
import com.example.protocol.DiscoveryProtocol
import DiscoveryProtocol.StartDiscovery
import com.example.actors.DiscoveryActor

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
object ApplicationMain extends App {
  implicit val system = ActorSystem("MyActorSystem")
  val MULTICAST_ADDR = "239.255.255.250"
  val MULTICAST_PORT = 0
  val udp = IO(Udp)
  val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR,MULTICAST_PORT,udp))
  //val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR, "en0", MULTICAST_PORT))
  val startTime = System.currentTimeMillis()
  discovery ! StartDiscovery()
  // This example app will ping pong 3 times and thereafter terminate the ActorSystem - 
  // see counter logic in PingActor
  Await.result(system.whenTerminated,10 minutes)
  println(s"Discovery completed in ${System.currentTimeMillis() - startTime} ms")
}