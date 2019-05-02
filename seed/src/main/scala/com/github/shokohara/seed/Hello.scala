package com.github.shokohara.seed

import better.files._

import scala.sys.process._
import org.json4s._
import org.json4s.Xml.{toJson, toXml}
import File._
import io.circe.generic.extras.semiauto._
import org.json4s.jackson.JsonMethods.{compact, render}
import io.circe.parser._
import cats._
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}

import scala.xml.XML

case class Data(nonRealTimeMeta: NonRealTimeMeta)

object Data {
  implicit val conf = Configuration.default.copy(transformMemberNames = a => a.head.toUpper + a.tail)
  implicit val decoder: Decoder[Data] = deriveDecoder[Data]
}
case class NonRealTimeMeta(acquisitionRecord: AcquisitionRecord)

object NonRealTimeMeta {
  implicit val conf = Configuration.default.copy(transformMemberNames = a => a.head.toUpper + a.tail)
  implicit val decoder: Decoder[NonRealTimeMeta] = deriveDecoder[NonRealTimeMeta]
}
case class AcquisitionRecord(group: List[Group])

object AcquisitionRecord {
  implicit val conf = Configuration.default.copy(transformMemberNames = a => a.head.toUpper + a.tail)
  implicit val decoder: Decoder[AcquisitionRecord] = deriveDecoder[AcquisitionRecord]
}

final case class Item(name: String, value: String)

object Item {
  implicit val conf: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder = io.circe.generic.extras.semiauto.deriveDecoder[Item]
}

case class Group(name: String, item: List[Item])

object Group {
  implicit val encodeBar: Decoder[Group] = Decoder.instance(h =>
    for {
      name <- h.downField("name").as[String]
      item <- h.downField("Item").as[List[Item]]
    } yield Group(name, item))
}

object Hello extends Greeting {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def main(args: Array[String]): Unit = {
//    val mp4File: File = File(args.head)
    val mp4File: File = File("/Users/sho/tmp/C0031.MP4")
    val movFile: File = mp4File.parent / s"${mp4File.nameWithoutExtension}.MOV"
    val xmlFile: File = mp4File.parent / s"${mp4File.nameWithoutExtension}M01.XML"

    println(parse(compact(render(toJson(XML.loadFile(xmlFile.toJava))))).fold(throw _, identity).spaces2)
//    println(
//      parse(compact(render(toJson(XML.loadFile(xmlFile.toJava)))))
//        .fold(throw _, identity).hcursor.get("NonRealTimeMeta").right.get)
    val data = parse(compact(render(toJson(XML.loadFile(xmlFile.toJava))))).flatMap(_.as[Data]).fold(throw _, identity)
    val map = data.nonRealTimeMeta.acquisitionRecord.group
      .filter(_.name === "ExifGPS").flatMap(_.item).map(a => (a.name, a.value)).toMap
    val longitudeRaw: String = map("Longitude").replaceAll(":", " ")
    val longitudeRef: String = map("LongitudeRef")
    val latitudeRaw: String = map("Latitude").replaceAll(":", " ")
    val latitudeRef: String = map("LatitudeRef")
    val gpsLatitude = ""
    val gpsLongitude = ""
    val gpsPosition = s"""35 deg 40' 1.20" N, 139 deg 43' 28.20" E"""
    println(gpsLatitude)
    println(gpsLongitude)
    println(gpsPosition)
    println(("rm" :: "-r" :: movFile.pathAsString :: Nil).!!)
    val result2 =
      ("ffmpeg" :: "-i" :: mp4File.pathAsString :: "-vcodec" :: "copy" :: "-acodec" :: "copy" :: movFile.pathAsString :: Nil).!!
    val result3 =
      ("exiftool" :: s"""-QuickTime:GPS Latitude=$gpsLatitude""" :: s"-QuickTime:GPS Longitude=$gpsLongitude" :: s"-QuickTime:GPS Position=$gpsPosition" :: movFile.pathAsString :: Nil).!
    println(result3)
  }
}

trait Greeting {
  lazy val greeting: String = "hello"
}
