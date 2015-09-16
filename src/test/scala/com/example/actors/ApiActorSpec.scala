package com.example.actors

import java.net.URI

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import com.example.protocol.DiscoveryProtocol.{DiscoveryComplete, StartDiscovery}
import com.example.protocol.ApiActorProtocol.GetZones
import com.example.protocol.SonosProtocol.ZoneQuery
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

	def this() = this(ActorSystem("ApiActorSpec", ConfigFactory.parseString("""
  akka.loggers = ["akka.testkit.TestEventListener"]""")))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"When receiving ZonesRequest" should {
		"send StartDiscovery" in {
			val apiActor = TestActorRef(new TestApiActor)
			val discoveryProbe = TestProbe()
			stubCreateDiscoveryActor.when(*, *, *).returns(discoveryProbe.ref)
			apiActor ! GetZones()
			discoveryProbe.expectMsg(StartDiscovery())
		}
	}
	"When awaiting discovery" should {
		"perform zone query on discovery completed" in {
			val apiActor = TestActorRef(new TestApiActor)
			val sonosApiProbe = TestProbe()
			val expectedLocation = "somelocation"
			stubCreateDiscoveryActor.when(*, *, *).returns(TestProbe().ref)
			stubCreateSonosApiActor.when(*).returns(sonosApiProbe.ref)
			apiActor ! GetZones()
			apiActor ! DiscoveryComplete(expectedLocation)
			sonosApiProbe.expectMsg(ZoneQuery(testActor))
		}
	}
	"When awaiting zone response" should {
		"log if any other message is received" in {
			val apiActor = TestActorRef(new TestApiActor)
			val sonosApiProbe = TestProbe()
			val zoneGroups = List.empty[ZoneGroup]
			stubCreateDiscoveryActor.when(*, *, *).returns(TestProbe().ref)
			stubCreateSonosApiActor.when(*).returns(sonosApiProbe.ref)
			apiActor ! GetZones()
			apiActor ! DiscoveryComplete("anylocation")
			EventFilter.warning(occurrences = 1) intercept {
				apiActor ! OtherMessage()
			}
		}
	}
	"When ready" should {
		"not perform zone discovery" in {
			val apiActor = TestActorRef(new TestApiActor)
			val discoveryProbe = TestProbe()
			val expectedLocation = "somelocation"
			stubCreateDiscoveryActor.when(*, *, *).returns(discoveryProbe.ref)
			stubCreateSonosApiActor.when(*).returns(TestProbe().ref)
			apiActor ! GetZones()
			apiActor ! DiscoveryComplete(expectedLocation)
			discoveryProbe.expectMsg(StartDiscovery())
			apiActor ! GetZones()
			discoveryProbe.expectNoMsg()
		}
	}

	trait StubActorCreator extends ApiBridgeActorCreator { this: ApiActor =>
		override def createDiscoveryActor(multicastAddress: String, interface: String, multicastPort: Int): ActorRef =
			stubCreateDiscoveryActor(multicastAddress, interface, multicastPort)
		override def createSonosApiActor(sonosApiUri: URI): ActorRef =
			stubCreateSonosApiActor(sonosApiUri)
	}

	class TestApiActor extends ApiActor with StubActorCreator

	case class OtherMessage()

}
