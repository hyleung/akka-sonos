package com.example.actors

import akka.actor.Status.Failure
import akka.actor.{Props, Actor, ActorSystem}
import akka.http.scaladsl.model._
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import com.example.protocol.SonosProtocol.{SonosError, ZoneResponse, ZoneQuery}
import com.example.sonos.{ZoneGroupMember, ZoneGroup}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalamock.scalatest.MockFactory

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
	trait MockParser extends BodyParser {
		override def parseZoneResponse(body: String): Seq[ZoneGroup] = List(ZoneGroup(List(ZoneGroupMember("foo",Uri("http://127.0.0.1")))))
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
			apiActor ! ZoneQuery()
			this.expectMsgPF() {
				case ZoneResponse(r) => r.nonEmpty
				case _ => fail()
			}
		}
		"raise error for ZoneQuery InternalServerError" in {
			stub.when().returns((StatusCodes.InternalServerError,"error"))
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			apiActor ! ZoneQuery()
			this.expectMsgPF() {
				case SonosError() => true
				case _ => fail()
			}
		}
		"raise error for ZoneQuery ClientError" in {
			stub.when().returns((StatusCodes.BadRequest,"error"))
			val apiActor = TestActorRef(TestSonosApiActor.props("127.0.01"))
			apiActor ! ZoneQuery()
			this.expectMsgPF() {
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
