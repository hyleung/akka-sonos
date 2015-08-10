package com.example.sonos


import akka.http.scaladsl.model.Uri

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-09
 * Time: 1:10 PM
 * To change this template use File | Settings | File Templates.
 */
case class ZoneGroup(members:Seq[ZoneGroupMember])
case class ZoneGroupMember(zoneName:String, location:Uri)
