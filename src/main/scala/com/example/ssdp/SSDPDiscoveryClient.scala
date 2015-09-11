package com.example.ssdp

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.io.Udp
import akka.util.ByteString

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-11
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
trait SSDPDiscoveryClient {
	val SEARCH = SSDPDiscoveryRequest(Map(
		"HOST" -> "239.255.255.250:1900",
		"MAN" -> "\"ssdp:discover\"",
		"MX" -> "1",
		"ST" -> " urn:schemas-upnp-org:device:ZonePlayer:1"
	)).serialize

	def sendSearchDatagram(actor: ActorRef, socket: InetSocketAddress): Unit = {
		val data: ByteString = ByteString(SEARCH, "UTF-8")
		actor ! Udp.Send(data, socket)
		actor ! Udp.Send(data, socket)
		actor ! Udp.Send(data, socket)
	}
}
