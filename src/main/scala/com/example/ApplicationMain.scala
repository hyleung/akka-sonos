package com.example

import akka.http.scaladsl.Http
import com.example.server.Server
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.language.postfixOps

object ApplicationMain extends App with LazyLogging with Server {
	val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
	println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
	StdIn.readLine()

	bindingFuture
		.flatMap(_.unbind())
		.onComplete(_ => system.terminate())

}
