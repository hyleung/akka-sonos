package com.example.ssdp

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Udp
import akka.testkit.{TestProbe, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-11
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
class SSDPDiscoveryClientSpec(_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike
	with Matchers
	with MockFactory
	with BeforeAndAfterAll
	with SSDPDiscoveryClient{

	def this() = this(ActorSystem("SSDPDiscoveryClientSpec"))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}
	"sendSearchDataGram" should {
		"send datagram" in {
			val udp = TestProbe()
			val socketAddress = InetSocketAddress.createUnresolved("somehost",80)
			sendSearchDatagram(udp.ref,socketAddress)
			udp.expectMsgPF(){
				case Udp.Send(_, socketAdress,_) =>
				case _ => fail()
			}
		}
		"send three" in {
			val udp = TestProbe()
			val socketAddress = InetSocketAddress.createUnresolved("somehost",80)
			sendSearchDatagram(udp.ref,socketAddress)
			udp.receiveN(3)
		}
	}

}
