package com.example.actors

import akka.actor.{ActorSystem, Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.example.protocol.SonosProtocol.{ZoneQuery, ZoneResponse}
import com.example.sonos.{ZoneGroupMember, ZoneGroup, SonosCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.xml.{Node, XML}

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
    with BodyParser {
  override def receive: Receive = {
    case ZoneQuery() => {
      val message = SonosCommand("ZoneGroupTopology", 1, "GetZoneGroupState", Map.empty)
      val entity: Strict = HttpEntity(ContentType(MediaTypes.`text/xml`), message.soapXml.toString())
      val headers: List[RawHeader] = List(RawHeader(SonosCommand.SOAP_ACTION_HEADER, message.actionHeader))
      val s = sender()
      execPost(s"$baseUri/ZoneGroupTopology/Control", entity, headers).onComplete{
        case Success((StatusCodes.OK, body))  => {
            s ! ZoneResponse(parseZoneResponse(body))
        }
        case Failure(err) => ???
      }
    }
    case _ => ???
  }


}

object SonosApiActor {
  def props(ip: String) = Props(new SonosApiActor(ip))
}

trait HttpClient { this: Actor =>
  implicit val materializer = ActorMaterializer()
  def execPost(uriString: String, httpEntity:RequestEntity, httpHeaders: List[HttpHeader]): Future[(StatusCode,String)] = {
    val httpReq = Http(context.system)
        .singleRequest(
            HttpRequest(uri = uriString, method = HttpMethods.POST, entity = httpEntity, headers = httpHeaders))
    for {
      response <- httpReq
      entity <- response.entity.toStrict(5 seconds)
    } yield (response.status, entity.data.decodeString("UTF-8"))
  }
}

trait BodyParser {
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