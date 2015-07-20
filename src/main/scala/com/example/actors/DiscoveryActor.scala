package com.example.actors

import java.net._
import java.nio.channels.DatagramChannel

import akka.actor.{PoisonPill, Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}
import akka.io.Udp
import akka.util.ByteString
import com.example.protocol.DiscoveryProtocol
import DiscoveryProtocol._
import com.example.ssdp.{SSDPDatagram, SSDPDiscoveryNotification, SSDPDiscoveryRequest}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created with IntelliJ IDEA.
 * Date: 15-06-26
 * Time: 9:54 PM
 * To change this template use File | Settings | File Templates.
 */
class DiscoveryActor(address:String, interface:Option[String], port:Int, udp:ActorRef) extends Actor{
	val log = Logging(context.system, this)
	var broadcastCount = 0
	val SEARCH = SSDPDiscoveryRequest(Map(
	"HOST" -> "239.255.255.250:1900",
	"MAN" -> "\"ssdp:discover\"",
	"MX" -> "1",
	"ST" -> " urn:schemas-upnp-org:device:ZonePlayer:1"
	)).serialize
	val opts = List(InetV4ProtocolFamily(), MulticastGroup(address, interface))
	//send a message to bind this actor to the socket address
	def receive:Receive = {
		case StartDiscovery() =>
			val socketAddress = new InetSocketAddress(InetAddress.getLocalHost, port)
			val remote = new InetSocketAddress(address, 1900)
			udp ! Udp.Bind(self, socketAddress,opts)
			context.become(awaitUdpBind(sender(), remote))
	}
	def awaitUdpBind(s:ActorRef, remote:InetSocketAddress): Receive = {
		case Udp.Bound(local) =>
			log.info(s"Bound to $local, awaiting discovery...")
			sendSearchDatagram(sender(),remote)
			context.become(ready(sender(), remote))
			context.system.scheduler.scheduleOnce(1000 milli,self, OnTimeout())
	}

	def ready(sender:ActorRef, socket:InetSocketAddress):Receive = {
		case OnTimeout() =>
			sendSearchDatagram(sender, socket)
			//schedule the next timeout
			context.system.scheduler.scheduleOnce(1000 milli,self, OnTimeout())
		case Udp.Received(data,socketAddress) =>
			val response = data.decodeString("UTF-8")
			//attempt to deserialize as a Discovery notification
			if (response.contains("Sonos")) {
				SSDPDatagram.deserialize[SSDPDiscoveryNotification](response) match {
					case Some(v) =>
						log.info(s"LOCATION found ${v.headers("LOCATION")}")
						log.debug(s"Resend count: $broadcastCount")
						sender ! DiscoveryComplete(v.headers("LOCATION"))
						self ! PoisonPill
						//context.system.terminate()
					case None =>
						//no op
						log.warning(s"Unable to deserialize: $response")
				}
			}

		case Udp.Unbind =>  sender ! Udp.Unbind
		case Udp.Unbound => context.stop(self)
	}

	def sendSearchDatagram(actor: ActorRef,  socket:InetSocketAddress): Unit = {
		val data: ByteString = ByteString(SEARCH, "UTF-8")
		actor ! Udp.Send(data, socket)
		actor ! Udp.Send(data, socket)
		actor ! Udp.Send(data, socket)
		broadcastCount += 1
	}

	def times(f:() => Unit, repeat:Int): Unit = {
		(1 to repeat) foreach( _ => f)
	}

}
//UDP Multi-cast
final case class InetV4ProtocolFamily() extends DatagramChannelCreator {
	@throws[Exception](classOf[Exception])
	override def create(): DatagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
}
//Multi-cast group settings
final case class MulticastGroup(address:String, interface:Option[String]) extends SocketOptionV2 with LazyLogging {
	override def afterBind(socket: DatagramSocket): Unit = {
		val group = InetAddress.getByName(address)
		val networkInterface:Seq[NetworkInterface] = interface match {
			case Some(iface) => Seq(NetworkInterface.getByName(iface))
			case None =>
				import scala.collection.JavaConversions._
				val interfaces = NetworkInterface.getNetworkInterfaces filter  { i => i.isUp && i.supportsMulticast()}
				interfaces.toSeq
		}
		networkInterface foreach { interface =>
			logger.debug("Joining multi-cast group {} on {}", address, interface.getName)
			socket.getChannel.join(group, interface)
		}

	}
}


object DiscoveryActor {
	def props(address:String, interface:String, port:Int, udp:ActorRef) = Props(new DiscoveryActor(address, Some(interface),port, udp))
	def props(address:String, port:Int, udp:ActorRef) = Props(new DiscoveryActor(address, None, port, udp))
}