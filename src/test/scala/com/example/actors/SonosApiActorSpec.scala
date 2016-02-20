package com.example.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.model._
import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import com.example.http.HttpClient
import com.example.protocol.SonosProtocol.{SonosError, ZoneQuery, ZoneResponse}
import com.example.sonos.{SonosResponseParser, ZoneGroup, ZoneGroupMember}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

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
	with MockFactory
	with BeforeAndAfterAll {

	val stub = stubFunction[(StatusCode, String)]
	trait MockHttpClient extends HttpClient { this: Actor =>
		override def execPost(uriString: String, httpEntity: RequestEntity, httpHeaders: List[HttpHeader]): Future[(StatusCode, String)] = Future {
			stub()
		}
	}
	trait MockParser extends SonosResponseParser {
		override def parseZoneResponse(body: String): Seq[ZoneGroup] = List(ZoneGroup(List(ZoneGroupMember("foo",Uri("http://127.0.0.1"))), "coordinator-id"))
	}
	class TestSonosApiActor(ip:String)
		extends SonosApiActor(ip)
		with MockHttpClient
		with MockParser

	object TestSonosApiActor {
		def props(ip: String) = Props(new TestSonosApiActor(ip))
	}

	def this() = this(ActorSystem("SonosApiActorSpec"))
	"SonosApiActor" must {
		"respond to ZoneQuery success" in  {
			stub.when().returns((StatusCodes.OK,"foo"))
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			val sender = TestProbe()
			apiActor ! ZoneQuery(sender.ref)
			sender.expectMsgPF() {
				case ZoneResponse(r) => r.nonEmpty
				case _ => fail()
			}
		}
		"raise error for ZoneQuery InternalServerError" in {
			stub.when().returns((StatusCodes.InternalServerError,"error"))
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			val sender = TestProbe()
			apiActor ! ZoneQuery(sender.ref)
			sender.expectMsgPF() {
				case SonosError() => true
				case _ => fail()
			}
		}
		"raise error for ZoneQuery ClientError" in {
			stub.when().returns((StatusCodes.BadRequest,"error"))
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			val sender = TestProbe()
			apiActor ! ZoneQuery(sender.ref)
			sender.expectMsgPF() {
				case SonosError() => true
				case _ => fail()
			}
		}
		"throw exception for other messages" in {
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			intercept[NotImplementedError] {apiActor.receive(Dummy())}
		}
	}

	override protected def afterAll(): Unit = {

	}

	case class Dummy()
}
