package com.example.actors

import akka.actor.{Props, Actor, ActorSystem}
import akka.http.scaladsl.model._
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import com.example.protocol.SonosProtocol.{ZoneResponse, ZoneQuery}
import com.example.sonos.{ZoneGroupMember, ZoneGroup}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-22
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
class SonosApiActorSpec(_system: ActorSystem)
	extends TestKit(_system)
	with ImplicitSender
	with WordSpecLike
	with Matchers
	with BeforeAndAfterAll {

	trait MockHttpClient extends HttpClient { this: Actor =>
		override def execPost(uriString: String, httpEntity: RequestEntity, httpHeaders: List[HttpHeader]): Future[(StatusCode, String)] = Future {
			(StatusCodes.OK, "foo")
		}
	}
	trait MockParser extends BodyParser {
		override def parseZoneResponse(body: String): Seq[ZoneGroup] = List(ZoneGroup(List(ZoneGroupMember("foo",Uri("http://127.0.0.1")))))
	}
	class TestSonosApiActor(ip:String) extends SonosApiActor(ip) with MockHttpClient with MockParser
	object TestSonosApiActor {
		def props(ip: String) = Props(new TestSonosApiActor(ip))
	}
	def this() = this(ActorSystem("SonosApiActorSpec"))
	"SonosApiActor" must {
		"respond to ZoneQuery success" in  {
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			apiActor ! ZoneQuery()
			this.expectMsgPF(250 millis,"Expect success response") {
				case ZoneResponse(r) => r.nonEmpty
				case _ => fail()
			}
		}
	}

}
