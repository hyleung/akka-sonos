package com.example

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val MULTICAST_ADDR = "239.255.255.250"
  val MULTICAST_PORT = 1900

  val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR, MULTICAST_PORT))
  //val discovery = system.actorOf(DiscoveryActor.props(MULTICAST_ADDR, "en0", MULTICAST_PORT))
  val startTime = System.currentTimeMillis()
  discovery ! StartDiscovery()
  // This example app will ping pong 3 times and thereafter terminate the ActorSystem - 
  // see counter logic in PingActor
  Await.result(system.whenTerminated,10 minutes)
  println(s"Discovery completed in ${System.currentTimeMillis() - startTime} ms")
}