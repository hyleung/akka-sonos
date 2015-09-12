package com.example.actors

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Udp
import akka.testkit._
import akka.util.ByteString
import com.example.protocol.DiscoveryProtocol._
import com.example.ssdp.{SSDPDiscoveryClient, SSDPDatagram, SSDPDiscoveryNotification, SSDPDiscoveryRequest}
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class DiscoveryActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike
with Matchers
with MockFactory
with BeforeAndAfterAll {

	def this() = this(ActorSystem("DiscoveryActorSpec", ConfigFactory.parseString("""
  akka.loggers = ["akka.testkit.TestEventListener"]""")))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"DiscoveryActor" must {
		val udp = TestProbe()
		val mock = mockFunction[Unit]
		val actor = TestActorRef(new TestDiscoveryActor(mock, udp.ref))
		val local = InetSocketAddress.createUnresolved("localhost", 1900)
		val remote = InetSocketAddress.createUnresolved("remote", 1900)
		"perform UDP bind on StartDiscovery" in {
			actor ! StartDiscovery()
			val options = udp.expectMsgPF() {
				case Udp.Bind(_, _, o) => o
			}
			assert(options.exists(o => o.isInstanceOf[MulticastGroup]))
			assert(options.exists(o => o.isInstanceOf[InetV4ProtocolFamily]))
		}
		"then send multi-cast on bind" in {
			//upon binding, discovery requests are sent back to the sender of the Udp.Bound message
			mock.expects().atLeastOnce()
			actor ! Udp.Bound(local)
		}
		"then respond with DiscoveryComplete after SSDP notification received" in {
			val data = """HTTP/1.1 200 OK
			 CACHE-CONTROL: max-age = 1800
			 LOCATION: http://192.168.1.70:1400/xml/device_description.xml
			 ST: urn:schemas-upnp-org:device:ZonePlayer:1
			 USN: uuid:RINCON_B8E93781D11001400::urn:schemas-upnp-org:device:ZonePlayer:1
			 X-RINCON-BOOTSEQ: 28
			 X-RINCON-HOUSEHOLD: Sonos_iROH6kmkXYSpfYZTTyCYZMC6jH"""
			actor ! Udp.Received(ByteString(data, "UTF-8"), local)
			expectMsgPF() {
				case DiscoveryComplete(location) => location should be("http://192.168.1.70:1400/xml/device_description.xml")
				case _ => fail()
			}
		}
	}
	"DiscoveryActor" must {
		"re-send search datagram on timeout" in {
			val udp = TestProbe()
			val mockSsdpClient = mockFunction[Unit]
			val actor = TestActorRef(new TestDiscoveryActor(mockSsdpClient, udp.ref))
			val local = InetSocketAddress.createUnresolved("localhost", 1900)
			val remote = InetSocketAddress.createUnresolved("remote", 1900)
			mockSsdpClient.expects().atLeastOnce()
			actor ! StartDiscovery()
			actor ! Udp.Bound(local)
			actor ! OnTimeout()
		}
	}

	"DiscoveryActor" must {
		"log if unable to deserialize sonos response" in {
			val udp = TestProbe()
			val stubSsdpClient = stubFunction[Unit]
			val actor = TestActorRef(new TestDiscoveryActor(stubSsdpClient,udp.ref))
			val local = InetSocketAddress.createUnresolved("localhost", 1900)
			val remote = InetSocketAddress.createUnresolved("remote", 1900)
			actor ! StartDiscovery()
			actor ! Udp.Bound(local)
			EventFilter.warning(occurrences = 1) intercept {
				actor ! Udp.Received(ByteString("Sonos", "UTF-8"), local)
			}
		}
	}
	"DiscoveryActor" must {
		"send udp unbind" in {
			val udp = TestProbe()
			val stubSsdpClient = stubFunction[Unit]
			val actor = TestActorRef(new TestDiscoveryActor(stubSsdpClient,udp.ref))
			val local = InetSocketAddress.createUnresolved("localhost", 1900)
			val remote = InetSocketAddress.createUnresolved("remote", 1900)
			actor ! StartDiscovery()
			actor ! Udp.Bound(local)
			actor ! Udp.Unbind
			expectMsg(Udp.Unbind)
		}
		"send udp unbound" in {
			val udp = TestProbe()
			val stubSsdpClient = stubFunction[Unit]
			val actor = TestActorRef(new TestDiscoveryActor(stubSsdpClient,udp.ref))
			val local = InetSocketAddress.createUnresolved("localhost", 1900)
			val remote = InetSocketAddress.createUnresolved("remote", 1900)
			actor ! StartDiscovery()
			actor ! Udp.Bound(local)
			actor ! Udp.Unbound
			//how to assert stop?
		}
	}

	trait MockSsdpClient extends SSDPDiscoveryClient {
		this: TestDiscoveryActor =>
		val fake: () => Unit

		override def sendSearchDatagram(actor: ActorRef, socket: InetSocketAddress): Unit = fake()
	}

	class TestDiscoveryActor(f: () => Unit, udp: ActorRef)
		extends DiscoveryActor("239.255.255.250", Some("en0"), 1900, udp) with MockSsdpClient {
		override val fake: () => Unit = f
	}

}
