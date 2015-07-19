package com.example.sonos

import org.scalatest.{FlatSpec, Matchers}

import scala.xml.Node

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-11
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosCommandSpec extends FlatSpec with Matchers {
	behavior of "SonosCommand"
	it should "return correct header value" in {
		val command = SonosCommand("ZoneGroupTopology",1,"GetZoneGroupState",Map.empty)
		command.actionHeader should be ("urn:schemas-upnp-org:service:serviceType:ZoneGroupTopology:1#GetZoneGroupState")
	}
	it should "have empty body" in {
		val command = SonosCommand("SomeEndpoint",1,"SomeAction",Map.empty)
		val soapXml = command.soapXml
		(soapXml \\ "foo").headOption should be (None)
	}
	it should "have soap body with args" in {
		val command = SonosCommand("SomeEndpoint",1,"SomeAction",Map("foo" -> "bar","fizz" -> "buzz"))
		val soapXml = command.soapXml
		(soapXml \\ "foo").headOption should be (Some(<foo>bar</foo>))
		(soapXml \\ "fizz").headOption should be (Some(<fizz>buzz</fizz>))
	}
	it should "have action element" in {
		val command = SonosCommand("SomeEndpoint",1,"SomeAction",Map("foo" -> "bar","fizz" -> "buzz"))
		val soapXml = command.soapXml
		//println(soapXml)
		val headOption: Option[Node] = (soapXml \\ "SomeAction").headOption
		headOption.nonEmpty should be (true)
		headOption.get.namespace should be (command.serviceTypeNamespace)
	}
	it should "have envelope element at root" in {
		val command = SonosCommand("SomeEndpoint",1,"SomeAction",Map.empty)
		val soapXml = command.soapXml
		println(soapXml)
		soapXml.namespace should be ("http://schemas.xmlsoap.org/soap/envelope/")
		soapXml.label should be ("Envelope")
	}
}
