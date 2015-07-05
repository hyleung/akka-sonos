package com.example.actors

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Udp
import akka.testkit._
import akka.util.ByteString
import com.example.protocol.DiscoveryProtocol._
import com.example.ssdp.{SSDPDatagram, SSDPDiscoveryNotification, SSDPDiscoveryRequest}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps
class DiscoveryActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("DiscoveryActorSpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "DiscoveryActor" must {
    val udp = TestProbe()
    val actor = TestActorRef(DiscoveryActor.props("239.255.255.250","en0",1900,udp.ref))
    val local = InetSocketAddress.createUnresolved("localhost",1900)
    val remote = InetSocketAddress.createUnresolved("remote",1900)
    "perform UDP bind on StartDiscovery" in {
      actor ! StartDiscovery()
      val options = udp.expectMsgPF(1 second,"Should bind to multi-cast group"){
        case Udp.Bind(_,_,o) => o
      }
      assert(options.exists(o => o.isInstanceOf[MulticastGroup]))
      assert(options.exists(o => o.isInstanceOf[InetV4ProtocolFamily]))
    }
    "then send multi-cast on bind" in {
      actor ! Udp.Bound(local)
      //upon binding, discovery requests are sent back to the sender of the Udp.Bound message
      val discoveryRequests = receiveWhile(100 millis, 100 millis,10){
        case Udp.Send(b,_,_) => SSDPDatagram.deserialize[SSDPDiscoveryRequest](b.decodeString("UTF-8")).isDefined
      }
      assert(discoveryRequests.size == 3)
    }
    "then terminate after SSDP notification received" in {
      val data ="""HTTP/1.1 200 OK
			 CACHE-CONTROL: max-age = 1800
			 LOCATION: http://192.168.1.70:1400/xml/device_description.xml
			 ST: urn:schemas-upnp-org:device:ZonePlayer:1
			 USN: uuid:RINCON_B8E93781D11001400::urn:schemas-upnp-org:device:ZonePlayer:1
			 X-RINCON-BOOTSEQ: 28
			 X-RINCON-HOUSEHOLD: Sonos_iROH6kmkXYSpfYZTTyCYZMC6jH"""
      val msg = SSDPDatagram.deserialize[SSDPDiscoveryNotification](data).get
      actor ! Udp.Received(ByteString(msg.serialize,"UTF-8"), local)
      //system should terminate
    }
  }

}
