package com.example.protocol

import akka.util.ByteString

object DiscoveryProtocol {
	case class StartDiscovery()
	case class OnTimeout()
	case class Processed(data:ByteString)
}


