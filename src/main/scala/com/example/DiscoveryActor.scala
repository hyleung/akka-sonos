package com.example

import java.net._
import java.nio.channels.DatagramChannel

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
class DiscoveryActor(address:String, interface:String, port:Int) extends Actor{

	val SEARCH = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:"+port+"\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1"

	import context.system
	val opts = List(InetV4ProtocolFamily(), MulticastGroup(address, interface))
	//send a message to bind this actor to the socket address
	def receive:Receive = {
		case StartDiscovery() =>
			val socketAddress = new InetSocketAddress(address, port)
			IO(Udp) ! Udp.Bind(self, socketAddress,opts)
			context.become(awaitUdpBind(socketAddress))
	}
	def awaitUdpBind(socket: InetSocketAddress): Receive = {
		case Udp.Bound(local) =>
			println("Bound, awaiting discovery...")
			sendSearchDatagram(socket)
			context.become(ready(sender(), socket))
			context.system.scheduler.scheduleOnce(50 milli,self, OnTimeout())
	}

	def ready(sender:ActorRef, socket:InetSocketAddress):Receive = {
		case OnTimeout() =>
			//println("Resending search datagram")
			sendSearchDatagram(socket)
			//schedule the next timeout
			context.system.scheduler.scheduleOnce(50 milli,self, OnTimeout())
		case Udp.Received(data,socketAddress) =>
			val response = data.decodeString("UTF-8")
			if ((response contains "Sonos")  && (response contains "X-RINCON-HOUSEHOLD")) {
				println(response)
				context.system.terminate()
			}
		case Udp.Unbind => sender ! Udp.Unbind
		case Udp.Unbound => context.stop(self)
	}

	def sendSearchDatagram(socket: InetSocketAddress): Unit = {
		val data: ByteString = ByteString(SEARCH, "UTF-8")
		times(() => IO(Udp) ! Udp.Send(data, socket), 10)
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
final case class MulticastGroup(address:String, interface:String) extends SocketOptionV2 {
	override def afterBind(socket: DatagramSocket): Unit = {
		val group = InetAddress.getByName(address)
		val networkInterface = NetworkInterface.getByName(interface)
		socket.getChannel.join(group, networkInterface)
	}
}

case class StartDiscovery()
case class OnTimeout()
case class Processed(data:ByteString)

object DiscoveryActor {
	def props(address:String, interface:String, port:Int) = Props(new DiscoveryActor(address, interface, port))
}