package com.example.server

import akka.http.scaladsl.model.Uri
import com.example.sonos.{ZoneGroup, ZoneGroupMember}
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{JsString, JsValue}
import spray.json._
/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-13
 * Time: 9:04 AM
 * To change this template use File | Settings | File Templates.
 */
class ProtocolSpec extends FlatSpec with Matchers with Protocols{
	behavior of "Uri format serialization"
	it should "serialize to json" in {
		val expected: String = "http://localhost:8080"
		val uri = Uri(expected)
		val result = uri.toJson
		result should be (JsString(expected))
	}
	it should "deserialize back from json" in {
		val expected: String = "http://localhost:8080"
		val uri = Uri(expected)
		val serialized = uri.toJson
		val result = serialized.convertTo[Uri]
		result should be (uri)
	}
	behavior of "zone group member format"
	it should "serialize to json" in {
		val data = ZoneGroupMember("foo",Uri("http://localhost:8080"))
		val serialized = data.toJson
		serialized should not be null
	}
	behavior of "zone group  format"
	it should "serialize to json" in {
		val data = ZoneGroup(List(ZoneGroupMember("foo",Uri("http://localhost:8080"))))
		val serialized = data.toJson
		serialized should not be null
	}
}
