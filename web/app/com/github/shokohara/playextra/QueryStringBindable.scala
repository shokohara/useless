package com.github.shokohara.playextra

import java.time.LocalDate

object QueryStringBindable {
  import play.api.mvc.QueryStringBindable

  implicit def localDateQueryStringBindable(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[LocalDate] =
    new QueryStringBindable[LocalDate] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, LocalDate]] =
        stringBinder.bind(key, params).map(_.map(LocalDate.parse(_)))
      override def unbind(key: String, ld: LocalDate): String = ld.toString
    }
}
