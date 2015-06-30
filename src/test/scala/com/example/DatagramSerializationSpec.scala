package com.example

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by hyleung on 15-06-29.
 */
class DatagramSerializationSpec extends FlatSpec with Matchers{
  behavior of "SSDPDiscoveryRequest"
  val headers:Map[String,String] = Map(
    "HOST" -> "239.255.255.250:1900",
    "MAN" -> "\"ssdp:discover\"",
    "MX" -> "1",
    "ST" -> "urn:schemas-upnp-org:device:ZonePlayer:1"
  )
  it should "serialize to expected format" in {
    val request = SSDPDiscoveryRequest(headers)
    val expected = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1"
    request.serialize should be (expected)
  }
  it should "deserialize to request" in {
    val expected = SSDPDiscoveryRequest(headers)
    val data = expected.serialize
    val result = SSDPDatagram.deserialize[SSDPDiscoveryRequest](data).get
    result should be (expected)
  }
  it should "not deserialize" in {
    val data = "M-FOO * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1"
    val result = SSDPDatagram.deserialize[SSDPDiscoveryRequest](data)
    result should be (None)
  }
}
