package com.example.actors

import java.net.URI

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import com.example.protocol.ApiProtocol.ZonesRequest
import com.example.protocol.DiscoveryProtocol.StartDiscovery
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-11
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
class ApiBridgeSpec(_system: ActorSystem) extends TestKit(_system)
	with ImplicitSender
	with WordSpecLike
	with Matchers
	with MockFactory
	with BeforeAndAfterAll {

	val stubCreateDiscoveryActor = stubFunction[String,String,Int,ActorRef]
	val stubCreateSonosApiActor = stubFunction[URI,ActorRef]

	def this() = this(ActorSystem("SSDPDiscoveryClientSpec"))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"When receiving ZonesRequest" should {
		"send StartDiscovery" in {
			val apiActor = TestActorRef(new TestApiBridge)
			val discoveryProbe = TestProbe()
			stubCreateDiscoveryActor.when(*, *, *).returns(discoveryProbe.ref)
			apiActor ! ZonesRequest()
			discoveryProbe.expectMsg(StartDiscovery())
		}
	}

	trait StubActorCreator extends ApiBridgeActorCreator { this: ApiBridge =>
		override def createDiscoveryActor(multicastAddress: String, interface: String, multicastPort: Int): ActorRef =
			stubCreateDiscoveryActor(multicastAddress, interface, multicastPort)
		override def createSonosApiActor(sonosApiUri: URI): ActorRef =
			stubCreateSonosApiActor(sonosApiUri)
	}

	class TestApiBridge extends ApiBridge with StubActorCreator


}
