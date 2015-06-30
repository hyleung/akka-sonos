package com.example

import java.net._
import java.nio.channels.DatagramChannel

import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ Props, ActorRef, Actor}

import akka.io.{Udp, IO}
import akka.util.ByteString
import akka.io.Inet.{SocketOptionV2, DatagramChannelCreator}


/**
 * Created with IntelliJ IDEA.
 * Date: 15-06-26
 * Time: 9:54 PM
 * To change this template use File | Settings | File Templates.
 */
class DiscoveryActor(address:String, interface:Option[String], port:Int) extends Actor{
	val log = Logging(context.system, this)
	var broadcastCount = 0

	val SEARCH = SSDPDiscoveryRequest(Map(
	"HOST" -> "239.255.255.250:1900",
	"MAN" -> "\"ssdp:discover\"",
	"MX" -> "1",
	"ST" -> " urn:schemas-upnp-org:device:ZonePlayer:1"
	)).serialize

	import context.system
	val opts = List(InetV4ProtocolFamily(), MulticastGroup(address, interface))
	//send a message to bind this actor to the socket address
	def receive:Receive = {
		case StartDiscovery() =>
			val socketAddress = new InetSocketAddress(port)
			IO(Udp) ! Udp.Bind(self, socketAddress,opts)
			context.become(awaitUdpBind(socketAddress))
	}
	def awaitUdpBind(socket: InetSocketAddress): Receive = {
		case Udp.Bound(local) =>
			println("Bound, awaiting discovery...")
			sendSearchDatagram(socket)
			context.become(ready(sender(), socket))
			context.system.scheduler.scheduleOnce(1000 milli,self, OnTimeout())
	}

	def ready(sender:ActorRef, socket:InetSocketAddress):Receive = {
		case OnTimeout() =>
			//println("Resending search datagram")
			sendSearchDatagram(socket)
			//schedule the next timeout
			context.system.scheduler.scheduleOnce(1000 milli,self, OnTimeout())
		case Udp.Received(data,socketAddress) =>
			val response = data.decodeString("UTF-8")
			if ((response contains "Sonos")  && (response contains "X-RINCON-HOUSEHOLD")) {
				SSDPDatagram.deserialize[SSDPDiscoveryNotification](response) match {
					case Some(v) =>
						println(s"LOCATION found ${v.headers("LOCATION")}")
						println(s"Resend count: $broadcastCount")
						context.system.terminate()
					case None => println("...still waiting")
				}
			}
		case Udp.Unbind => sender ! Udp.Unbind
		case Udp.Unbound => context.stop(self)
	}

	def sendSearchDatagram(socket: InetSocketAddress): Unit = {
		val data: ByteString = ByteString(SEARCH, "UTF-8")
		times(() => IO(Udp) ! Udp.Send(data, socket), 3)
		broadcastCount += 1
	}

	def times(f:() => Unit, repeat:Int): Unit = {
		(1 to repeat) foreach( _ => f)
	}

}
//UDP Multicast
final case class InetV4ProtocolFamily() extends DatagramChannelCreator {
	@throws[Exception](classOf[Exception])
	override def create(): DatagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
}
//Multicast group settings
final case class MulticastGroup(address:String, interface:Option[String]) extends SocketOptionV2 {
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
			println(s"Joining group on ${interface.getDisplayName}")
			socket.getChannel.join(group, interface)
		}

	}
}

case class StartDiscovery()
case class OnTimeout()
case class Processed(data:ByteString)

object DiscoveryActor {
	def props(address:String, interface:String, port:Int) = Props(new DiscoveryActor(address, Some(interface), port))
	def props(address:String, port:Int) = Props(new DiscoveryActor(address, None, port))
}