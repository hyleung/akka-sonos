package com.example.sonos


import org.scalatest.{FlatSpec, Matchers}
import org.xml.sax.SAXParseException
import scala.io.Source
/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-10
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosResponseParserSpec extends FlatSpec with Matchers with SonosResponseParser{
	behavior of "parseZoneResponse"
	it should "successfully return Seq of ZoneGroup" in {
		val source = Source.fromURL(getClass().getResource("/zoneGroupResponseSample.xml"))
		val xml = source.mkString
		val result = parseZoneResponse(xml)
		result should not be empty
	}
	it should "Throw exception if unable to parse XML" in {
		val xml = "<some><other/><xml/></some>"
		a [SAXParseException] should be thrownBy parseZoneResponse(xml)
	}
}
