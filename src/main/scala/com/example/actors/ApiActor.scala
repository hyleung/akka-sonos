package com.example.actors

import java.net.URI

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.{IO, Udp}
import com.example.protocol.DiscoveryProtocol.{DiscoveryComplete, StartDiscovery}
import com.example.protocol.ApiActorProtocol._
import com.example.protocol.SonosProtocol.ZoneQuery
import com.example.protocol.SonosRequest

/**
 * Created by hyleung on 15-07-24.
 */
class ApiActor extends Actor with ActorLogging with ApiBridgeActorCreator {
	implicit val _ = context.system
	val MULTICAST_ADDR = "239.255.255.250"
	val MULTICAST_PORT = 0
	val udp = IO(Udp)
	var sonosApiUri: URI = null

	override def receive: Receive = {
		case s:SonosRequest =>
			createDiscoveryActor(MULTICAST_ADDR, "en0", MULTICAST_PORT) ! StartDiscovery()
			context.become(awaitingDiscovery(sender(), s))
	}

	def awaitingDiscovery(sender:ActorRef, pendingRequest: SonosRequest): Receive = {
		case DiscoveryComplete(location) =>
			sonosApiUri = URI.create(location)
			context.become(discoveryComplete(sender))
			self ! pendingRequest

	}

	def discoveryComplete(sender:ActorRef):Receive = {
		case GetZones() =>
			createSonosApiActor(sonosApiUri) ! ZoneQuery(sender)
			context.become(ready)
	}

	def ready:Receive = {
		case GetZones() => createSonosApiActor(sonosApiUri) ! ZoneQuery(sender())

	}
}

trait ApiBridgeActorCreator {
	this: ApiActor =>
	def createDiscoveryActor(multicastAddress: String, interface: String, multicastPort: Int): ActorRef =
		context.actorOf(DiscoveryActor.props(multicastAddress, interface, multicastPort, IO(Udp)))

	def createSonosApiActor(sonosApiUri: URI): ActorRef =
		context.actorOf(SonosApiActor.props(s"http://${sonosApiUri.getHost}:${sonosApiUri.getPort}"))
}

object ApiActor {

}
