package com.example.actors

import java.net.URI

import akka.actor.{ActorLogging, PoisonPill, ActorRef, Actor}
import akka.io.{Udp, IO}
import com.example.protocol.DiscoveryProtocol.{DiscoveryComplete, StartDiscovery}
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.protocol.{SonosResponse, SonosRequest}

/**
 * Created by hyleung on 15-07-24.
 */
class ApiBridge extends Actor with ActorLogging with ApiBridgeActorCreator {
	implicit val _ = context.system
	val MULTICAST_ADDR = "239.255.255.250"
	val MULTICAST_PORT = 0
	val udp = IO(Udp)
	var sonosApiUri: URI = null

	override def receive: Receive = {
		case r:SonosRequest =>
			createDiscoveryActor(MULTICAST_ADDR, "en0", MULTICAST_PORT) ! StartDiscovery()
			context.become(awaitingDiscovery(sender(), r))
	}

	def awaitingDiscovery(sender: ActorRef, pendingRequest:SonosRequest): Receive = {
		case DiscoveryComplete(location) =>
			sonosApiUri = URI.create(location)
			createSonosApiActor(sonosApiUri) ! pendingRequest
			context.become(awaitingResponse(sender))
	}

	def awaitingResponse(sender: ActorRef): Receive = {
		case r:SonosResponse =>
			sender ! r
			context.become(receive)
		case other => log.warning(s"Unexpected message: ${other.toString}")
	}
}

trait ApiBridgeActorCreator {
	this: ApiBridge =>
	def createDiscoveryActor(multicastAddress: String, interface: String, multicastPort: Int): ActorRef =
		context.actorOf(DiscoveryActor.props(multicastAddress, interface, multicastPort, IO(Udp)))

	def createSonosApiActor(sonosApiUri: URI): ActorRef =
		context.actorOf(SonosApiActor.props(s"http://${sonosApiUri.getHost}:${sonosApiUri.getPort}"))
}

object ApiBridge {

}
