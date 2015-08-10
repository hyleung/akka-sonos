package com.example.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.example.protocol.SonosProtocol.{ZoneQuery, ZoneResponse}
import com.example.sonos.{ZoneGroupMember, ZoneGroup, SonosCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.xml.{Node, XML}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-17
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosApiActor(baseUri: String) extends Actor with ActorLogging {
  implicit val _ = context.system
  implicit val materializer = ActorMaterializer()

  override def receive: Receive = {
    case ZoneQuery() => {
      val message = SonosCommand("ZoneGroupTopology", 1, "GetZoneGroupState", Map.empty)
      val f = Http().singleRequest(HttpRequest(uri = s"$baseUri/ZoneGroupTopology/Control",
        method = HttpMethods.POST,
        entity = HttpEntity(ContentType(MediaTypes.`text/xml`), message.soapXml.toString()),
        headers = List(RawHeader(SonosCommand.SOAP_ACTION_HEADER, message.actionHeader))
      ))
      val s = sender()
      f.onSuccess {
        case resp if resp.status == StatusCodes.OK => resp.entity.toStrict(5 seconds).map { e =>
          val body = e.data.decodeString("UTF-8")
          s ! ZoneResponse(parseZoneResponse(body))
        }
        case other => ???
      }
    }
    case _ => ???
  }

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

object SonosApiActor {
  def props(ip: String) = Props(new SonosApiActor(ip))
}
