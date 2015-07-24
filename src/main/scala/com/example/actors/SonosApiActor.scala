package com.example.actors

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Props, Actor}
import akka.http.javadsl.model.headers.CustomHeader
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http._
import com.example.sonos.SonosCommand
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-17
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosApiActor(baseUri:String) extends Actor with ActorLogging{
	implicit val _ = context.system
	implicit val materializer = ActorMaterializer()
	override def receive: Receive = {
		case ZoneQuery() => {
			val message =  SonosCommand("ZoneGroupTopology",1,"GetZoneGroupState",Map.empty)
			val f = Http().singleRequest(HttpRequest(uri = s"$baseUri/ZoneGroupTopology/Control",
				method = HttpMethods.POST,
				entity = HttpEntity(ContentType(MediaTypes.`text/xml`), message.soapXml.toString()),
				headers = List(RawHeader(SonosCommand.SOAP_ACTION_HEADER,message.actionHeader))
			))
			f.map{ r =>   log.info(r.status.defaultMessage()); sender() ! ZoneResponse(r.entity.toString)}
		}
		case _ => ???
	}
}

object SonosApiActor {
	def props(ip:String) = Props(new SonosApiActor(ip))
}
