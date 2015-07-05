package com.example.ssdp

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by hyleung on 15-06-29.
 */
class DatagramSerializationSpec extends FlatSpec with Matchers{
  class DiscoveryRequestTest {
    val headers:Map[String,String] = Map(
      "HOST" -> "239.255.255.250:1900",
      "MAN" -> "\"ssdp:discover\"",
      "MX" -> "1",
      "ST" -> "urn:schemas-upnp-org:device:ZonePlayer:1"
    )
  }

  behavior of "SSDPDiscoveryRequest"
  it should "serialize to expected format" in  new DiscoveryRequestTest {
    val request = SSDPDiscoveryRequest(headers)
    val expected = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1"
    request.serialize should be (expected)
  }
  it should "deserialize to request" in new DiscoveryRequestTest{
    val expected = SSDPDiscoveryRequest(headers)
    val data = expected.serialize
    val result = SSDPDatagram.deserialize[SSDPDiscoveryRequest](data).get
    result should be (expected)
  }
  it should "not deserialize" in new DiscoveryRequestTest {
    val data = "M-FOO * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1"
    val result = SSDPDatagram.deserialize[SSDPDiscoveryRequest](data)
    result should be (None)
  }

  class DiscoveryNotificationTest {
    val headers:Map[String,String] = Map(
      "CACHE-CONTROL" ->  "max-age = 1800",
      "LOCATION" ->  "http://192.168.1.70:1400/xml/device_description.xml",
      "ST" -> "urn:schemas-upnp-org:device:ZonePlayer:1",
      "USN" -> "uuid:RINCON_B8E93781D11001400::urn:schemas-upnp-org:device:ZonePlayer:1",
      "X-RINCON-BOOTSEQ" -> "28",
      "X-RINCON-HOUSEHOLD" -> "Sonos_iROH6kmkXYSpfYZTTyCYZMC6jH"
    )
  }

  behavior of "SSDPDiscoveryNotification"
  it should "deserialize" in new DiscoveryNotificationTest{
    val data ="""HTTP/1.1 200 OK
			 CACHE-CONTROL: max-age = 1800
			 LOCATION: http://192.168.1.70:1400/xml/device_description.xml
			 ST: urn:schemas-upnp-org:device:ZonePlayer:1
			 USN: uuid:RINCON_B8E93781D11001400::urn:schemas-upnp-org:device:ZonePlayer:1
			 X-RINCON-BOOTSEQ: 28
			 X-RINCON-HOUSEHOLD: Sonos_iROH6kmkXYSpfYZTTyCYZMC6jH"""
    val result = SSDPDatagram.deserialize[SSDPDiscoveryNotification](data).get
    println(result)
    println(SSDPDiscoveryNotification(headers));
    result should be (SSDPDiscoveryNotification(headers))
  }
  it should "not deserialize" in new DiscoveryNotificationTest{
    val data ="""Foo * HTTP/1.1
                |HOST: 239.255.255.250:1900 """
    val result = SSDPDatagram.deserialize[SSDPDiscoveryNotification](data)
    result should be (None)
  }
}
