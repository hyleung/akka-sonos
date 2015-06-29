package com.example

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
  val method = "M-SEARCH * HTTP/1.1"
}

object SSDPDatagram {
  @annotation.implicitNotFound("Missing implicit convertor for your SSDPDatagram type.")
  trait SSDPLike[A <: SSDPDatagram] {
    def convert(s:String):A
  }
  implicit object SSDPDiscoveryConvertor extends SSDPLike[SSDPDiscoveryRequest] {
    override def convert(s: String): SSDPDiscoveryRequest = {
      val lines = s.lines.toSeq
      val headers = lines.tail.foldLeft(Map.empty[String,String]){ (acc,line) =>
        val splitIdx = line.indexOf(":")
        val p = line.splitAt(splitIdx)
        acc + (p._1 -> p._2.replaceFirst(":","").trim)
      }
      SSDPDiscoveryRequest(headers)
    }
  }
  def deserialize[A <: SSDPDatagram](data:String)(implicit convertor:SSDPLike[A]):A = convertor.convert(data)
}

