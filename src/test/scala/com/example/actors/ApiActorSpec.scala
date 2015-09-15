package com.example.actors

import java.net.URI

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import com.example.protocol.DiscoveryProtocol.{DiscoveryComplete, StartDiscovery}
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.sonos.ZoneGroup
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.testkit.EventFilter
/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-11
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
class ApiActorSpec(_system: ActorSystem) extends TestKit(_system)
	with ImplicitSender
	with WordSpecLike
	with Matchers
	with MockFactory
	with BeforeAndAfterAll {

	val stubCreateDiscoveryActor = stubFunction[String,String,Int,ActorRef]
	val stubCreateSonosApiActor = stubFunction[URI,ActorRef]

	def this() = this(ActorSystem("SSDPDiscoveryClientSpec", ConfigFactory.parseString("""
  akka.loggers = ["akka.testkit.TestEventListener"]""")))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"When receiving ZonesRequest" should {
		"send StartDiscovery" in {
			val apiActor = TestActorRef(new TestApiBridge)
			val discoveryProbe = TestProbe()
			stubCreateDiscoveryActor.when(*, *, *).returns(discoveryProbe.ref)
			apiActor ! ZoneQuery()
			discoveryProbe.expectMsg(StartDiscovery())
		}
	}
	"When awaiting discovery" should {
		"perform zone query on discovery completed" in {
			val apiActor = TestActorRef(new TestApiBridge)
			val sonosApiProbe = TestProbe()
			val expectedLocation = "somelocation"
			stubCreateDiscoveryActor.when(*, *, *).returns(TestProbe().ref)
			stubCreateSonosApiActor.when(*).returns(sonosApiProbe.ref)
			apiActor ! ZoneQuery()
			apiActor ! DiscoveryComplete(expectedLocation)
			sonosApiProbe.expectMsg(ZoneQuery())
		}
	}
	"When awaiting zone response" should {
		"send zone response to sender" in {
			val apiActor = TestActorRef(new TestApiBridge)
			val sonosApiProbe = TestProbe()
			val zoneGroups = List.empty[ZoneGroup]
			stubCreateDiscoveryActor.when(*, *, *).returns(TestProbe().ref)
			stubCreateSonosApiActor.when(*).returns(sonosApiProbe.ref)
			apiActor ! ZoneQuery()
			apiActor ! DiscoveryComplete("anylocation")
			apiActor ! ZoneResponse(zoneGroups)
			expectMsg(ZoneResponse(zoneGroups))
		}
		"log if any other message is received" in {
			val apiActor = TestActorRef(new TestApiBridge)
			val sonosApiProbe = TestProbe()
			val zoneGroups = List.empty[ZoneGroup]
			stubCreateDiscoveryActor.when(*, *, *).returns(TestProbe().ref)
			stubCreateSonosApiActor.when(*).returns(sonosApiProbe.ref)
			apiActor ! ZoneQuery()
			apiActor ! DiscoveryComplete("anylocation")
			EventFilter.warning(occurrences = 1) intercept {
				apiActor ! OtherMessage()
			}
		}
	}

	trait StubActorCreator extends ApiBridgeActorCreator { this: ApiBridge =>
		override def createDiscoveryActor(multicastAddress: String, interface: String, multicastPort: Int): ActorRef =
			stubCreateDiscoveryActor(multicastAddress, interface, multicastPort)
		override def createSonosApiActor(sonosApiUri: URI): ActorRef =
			stubCreateSonosApiActor(sonosApiUri)
	}

	class TestApiBridge extends ApiBridge with StubActorCreator

	case class OtherMessage()

}
