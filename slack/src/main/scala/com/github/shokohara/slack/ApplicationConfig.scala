package com.github.shokohara.slack

import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader}

case class ApplicationConfig(slackToken: NonEmptyString,
                             slackChannelName: NonEmptyString,
                             slackUserName: NonEmptyString)

object ApplicationConfig {
  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), useDefaultArgs = false, allowUnknownKeys = true)
  implicit val reader: ConfigReader[ApplicationConfig] = deriveReader
}
