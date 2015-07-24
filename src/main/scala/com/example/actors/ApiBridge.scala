package com.example.actors

import java.net.URI

import akka.actor.{ActorLogging, PoisonPill, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.io.{Udp, IO}
import com.example.protocol.DiscoveryProtocol.{DiscoveryComplete, StartDiscovery}
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.protocol.ApiProtocol.{ZonesResponse, ZonesRequest}

import scala.xml.{XML, Elem}

/**
 * Created by hyleung on 15-07-24.
 */
class ApiBridge extends Actor with ActorLogging{
  implicit val _ = context.system
  val MULTICAST_ADDR = "239.255.255.250"
  val MULTICAST_PORT = 0
  val udp = IO(Udp)
  var sonosApiUri:URI = null
  override def receive: Receive =  {
    case ZonesRequest() =>
      context.actorOf(DiscoveryActor.props(MULTICAST_ADDR, "en0", MULTICAST_PORT, udp)) ! StartDiscovery()
      context.become(awaitingDiscovery(sender()))
  }
  def awaitingDiscovery(sender:ActorRef): Receive = {
    case DiscoveryComplete(location) =>
      sonosApiUri = URI.create(location)
      context.actorOf(SonosApiActor.props(s"http://${sonosApiUri.getHost}:${sonosApiUri.getPort}")) ! ZoneQuery()
      context.become(awaitingResponse(sender))
  }
  def awaitingResponse(sender:ActorRef):Receive = {
    case ZoneResponse(body) =>
      val zoneGroupState = (XML.loadString(body) \\ "GetZoneGroupStateResponse" \ "ZoneGroupState").text
      sender ! ZonesResponse(zoneGroupState)
      self ! PoisonPill
    case other => log.warning(s"Unexpected message: ${other.toString}")
  }
}

object ApiBridge {

}
