package com.github.shokohara.slack

import cats.data.NonEmptyList
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader}

final case class ApplicationConfig(
  slackToken: NonEmptyString,
  slackChannelNames: NonEmptyList[NonEmptyString],
  slackUserName: NonEmptyString
)

object ApplicationConfig {

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), useDefaultArgs = false, allowUnknownKeys = true)
  implicit val reader: ConfigReader[ApplicationConfig] = deriveReader
}
