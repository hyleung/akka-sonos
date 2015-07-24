package com.example.protocol

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-17
 * Time: 9:43 PM
 * To change this template use File | Settings | File Templates.
 */
object SonosProtocol {
	case class ZoneQuery()
	case class ZoneResponse(body:String)
}
