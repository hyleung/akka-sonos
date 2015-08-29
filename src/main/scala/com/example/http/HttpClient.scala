package com.example.http

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-29
 * Time: 10:37 AM
 * To change this template use File | Settings | File Templates.
 */
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
