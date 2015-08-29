package com.example.sonos

import akka.http.scaladsl.model.Uri

import scala.xml.{Node, XML}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-29
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
trait SonosResponseParser {
  def parseZoneResponse(body:String):Seq[ZoneGroup] = {
    val zoneGroupState = XML.loadString(body) \\ "GetZoneGroupStateResponse" \ "ZoneGroupState"
    val zoneGroups = XML.loadString(zoneGroupState.text) \\ "ZoneGroup"
    zoneGroups.map(zoneGroupFromNode)
  }

  def zoneGroupFromNode(groupNode:Node):ZoneGroup = {
    val members = (groupNode \\ "ZoneGroupMember").map(groupMemberFromNode)
    ZoneGroup(members)
  }

  def groupMemberFromNode(memberNode:Node):ZoneGroupMember = {
    val location = Uri((memberNode \ "@Location").text)
    val zoneName = (memberNode \ "@ZoneName").text
    ZoneGroupMember(zoneName, location)
  }
}
