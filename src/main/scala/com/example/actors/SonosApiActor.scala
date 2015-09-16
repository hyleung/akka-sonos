package com.example.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.StatusCodes.Success
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.example.http.HttpClient
import com.example.protocol.SonosProtocol.{SonosError, ZoneQuery, ZoneResponse}
import com.example.sonos.{SonosCommand, SonosResponseParser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-17
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosApiActor(baseUri: String)
    extends Actor
    with ActorLogging
    with HttpClient
    with SonosResponseParser {
  override def receive: Receive = {
    case ZoneQuery(sender) => {
      val message = SonosCommand("ZoneGroupTopology", 1, "GetZoneGroupState", Map.empty)
      val entity: Strict = HttpEntity(ContentType(MediaTypes.`text/xml`), message.soapXml.toString())
      val headers: List[RawHeader] = List(RawHeader(SonosCommand.SOAP_ACTION_HEADER, message.actionHeader))
      execPost(s"$baseUri/ZoneGroupTopology/Control", entity, headers).map{
        case (Success(_), body)  => {
            sender ! ZoneResponse(parseZoneResponse(body))
        }
        case (code ,_) if code.isFailure() => sender ! SonosError()
      }
    }
    case _ => ???
  }
}

object SonosApiActor {
  def props(ip: String) = Props(new SonosApiActor(ip))
}
