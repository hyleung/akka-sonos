package com.example.protocol

import com.example.sonos.ZoneGroup

/**
 * Created by hyleung on 15-07-24.
 */
object ApiProtocol {
  case class ZonesRequest()
  case class ZonesResponse(zoneGroups:Seq[ZoneGroup])
}
