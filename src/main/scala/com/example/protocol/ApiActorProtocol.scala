package com.example.protocol

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-14
 * Time: 9:18 PM
 * To change this template use File | Settings | File Templates.
 */

trait SonosRequest {}

object ApiActorProtocol {
	case class GetZones() extends SonosRequest
}
