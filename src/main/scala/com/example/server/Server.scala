package com.example.server

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.example.actors.{ApiActor, ApiActor$}
import com.example.protocol.ApiActorProtocol.GetZones
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.sonos.{ZoneGroup, ZoneGroupMember}
import spray.json._
import akka.pattern.ask
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-13
 * Time: 8:52 AM
 * To change this template use File | Settings | File Templates.
 */
trait Server extends Protocols {
	implicit val system = ActorSystem("SonosApiBridge")
	implicit val materializer = ActorMaterializer()
	implicit val timeout:Timeout = Timeout(5000, TimeUnit.MILLISECONDS)
	val apiActor = system.actorOf(Props[ApiActor])
	val route =
		path("") {
			get {
				complete {
					(apiActor ? GetZones()).map[JsValue] {
						case ZoneResponse(g) => g.toJson
					}
				}
			}
		}
}

trait Protocols extends DefaultJsonProtocol {
	implicit object uriFormat extends RootJsonFormat[Uri] {
		override def write(obj: Uri): JsValue = JsString(obj.toString())
		override def read(json: JsValue): Uri = Uri(json.asInstanceOf[JsString].value)
	}
	implicit val zoneGroupMemberFormat:RootJsonFormat[ZoneGroupMember] = jsonFormat(ZoneGroupMember,"name", "uri")
	implicit val zoneGroupFormat:RootJsonFormat[ZoneGroup] = jsonFormat(ZoneGroup,"zone_members", "coordinator")
}

