package com.github.shokohara.slack

import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader}

case class ApplicationConfig(slackToken: String, slackChannelName: String, slackUserName: String)

object ApplicationConfig {
  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), useDefaultArgs = false, allowUnknownKeys = true)
  implicit val reader: ConfigReader[ApplicationConfig] = deriveReader
}
