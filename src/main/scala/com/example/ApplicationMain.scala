package com.example

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.example.actors.ApiBridge
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.sonos.{ZoneGroup, ZoneGroupMember}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.language.postfixOps

object ApplicationMain extends App with LazyLogging with Protocols {
	implicit val system = ActorSystem("SonosApiBridge")
	implicit val materializer = ActorMaterializer()
	implicit val timeout:Timeout = Timeout(5000, TimeUnit.MILLISECONDS)
	val apiActor = system.actorOf(Props[ApiBridge])
	val route =
		path("") {
			get {
				complete {
					(apiActor ? ZoneQuery()).map[JsValue] {
						case ZoneResponse(g) => g.toJson
					}
				}
			}
		}
	val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
	println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
	StdIn.readLine()

	bindingFuture
		.flatMap(_.unbind())
		.onComplete(_ => system.terminate())

}

trait Protocols extends DefaultJsonProtocol {
	implicit object uriFormat extends RootJsonFormat[Uri] {
		override def write(obj: Uri): JsValue = JsString(obj.toString())
		override def read(json: JsValue): Uri = Uri(json.toString())
	}
	implicit val zoneGroupMemberFormat:RootJsonFormat[ZoneGroupMember] = jsonFormat(ZoneGroupMember,"name", "uri")
	implicit val zoneGroupFormat:RootJsonFormat[ZoneGroup] = jsonFormat(ZoneGroup,"zone_members")
}