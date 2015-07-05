package com.example.ssdp

/**
 * Created by hyleung on 15-06-29.
 */
trait SSDPDatagram {
  val method:String
  val headers:Map[String,String]
  def serialize:String = {
    val entries = headers.foldLeft(Seq.empty[String]){(acc,entry) => acc :+ s"${entry._1}: ${entry._2}"}
    val headerString = entries mkString "\r\n"
    s"$method\r\n$headerString"
  }
}
case class SSDPDiscoveryRequest(headers:Map[String,String]) extends SSDPDatagram {
  val method = SSDPDiscoveryRequest.method
}

case class SSDPDiscoveryNotification(headers:Map[String,String]) extends SSDPDatagram {
  val method = SSDPDiscoveryRequest.method
}

object SSDPDiscoveryRequest {
  val method = "M-SEARCH * HTTP/1.1"
}

object SSDPDiscoveryNotification {
  val method = "HTTP/1.1 200 OK"
}

object SSDPDatagram {
  @annotation.implicitNotFound("Missing implicit converter for your SSDPDatagram type.")
  trait SSDPLike[A <: SSDPDatagram] {
    def convert(s:String):Option[A]
  }
  def parseHeaders(lines:Seq[String]) = {
    lines.foldLeft(Map.empty[String, String]) { (acc, line) =>
      val splitIdx = line.indexOf(":")
      val p = line.splitAt(splitIdx)
      acc + (p._1.trim -> p._2.replaceFirst(":", "").trim)
    }
  }
  implicit object SSDPDiscoveryConvertor extends SSDPLike[SSDPDiscoveryRequest] {
    override def convert(s: String): Option[SSDPDiscoveryRequest] = {
      val lines = s.lines.toSeq
      if (lines.head == SSDPDiscoveryRequest.method) {
        Some(SSDPDiscoveryRequest(parseHeaders(lines.tail)))
      } else {
        None
      }
    }
  }
  implicit object SSDPDiscoveryNotificationConvertor extends SSDPLike[SSDPDiscoveryNotification] {
    override def convert(s: String): Option[SSDPDiscoveryNotification] = {
      val lines = s.lines.toSeq
      if (lines.head == SSDPDiscoveryNotification.method) {
        Some(SSDPDiscoveryNotification(parseHeaders(lines.tail)))
      } else {
        None
      }
    }
  }
  def deserialize[A <: SSDPDatagram](data:String)(implicit convertor:SSDPLike[A]):Option[A] = convertor.convert(data)
}

