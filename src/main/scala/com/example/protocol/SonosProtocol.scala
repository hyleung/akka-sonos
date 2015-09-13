package com.example.protocol

import com.example.sonos.ZoneGroup

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-17
 * Time: 9:43 PM
 * To change this template use File | Settings | File Templates.
 */

sealed trait SonosRequest {}

sealed trait SonosResponse {}

object SonosProtocol {
	case class SonosError() extends SonosResponse
	case class ZoneQuery() extends SonosRequest
	case class ZoneResponse(zoneGroups:Seq[ZoneGroup]) extends SonosResponse
}
