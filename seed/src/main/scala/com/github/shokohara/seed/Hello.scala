package com.github.shokohara.seed

import java.util.UUID

import cats.effect.IO
import cats.free.Free
import hammock._
import hammock.apache.ApacheInterpreter
import org.apache.http.impl.client.HttpClientBuilder

object Hello extends Greeting with App {

  implicit val interpreter: ApacheInterpreter[IO] = new ApacheInterpreter[IO](HttpClientBuilder.create().build())

  def request(uri: Uri, method: Method)(implicit dc: UUID): IO[Unit] = {
    val response: Free[HttpF, HttpResponse] = Hammock.request(method, uri, Map.empty)
    response.exec[IO].flatMap(handler)
  }

  def dumpRequest(f: HttpF[HttpResponse])(implicit dc: UUID): Unit = {
    val (uri, headers, entity, method) = f match {
      case Get(HttpRequest(u, h, e))     => (u, h, e, "GET")
      case Options(HttpRequest(u, h, e)) => (u, h, e, "OPTIONS")
      case Head(HttpRequest(u, h, e))    => (u, h, e, "HEAD")
      case Post(HttpRequest(u, h, e))    => (u, h, e, "POST")
      case Put(HttpRequest(u, h, e))     => (u, h, e, "PUT")
      case Delete(HttpRequest(u, h, e))  => (u, h, e, "DELETE")
      case Trace(HttpRequest(u, h, e))   => (u, h, e, "TRACE")
      case Patch(HttpRequest(u, h, e))   => (u, h, e, "PATCH")
    }
    println(
      Map("uri" -> uri, "headers" -> headers, "method" -> method, "entity" -> entity, "dc" -> dc.toString).toString())
  }

  def handler(response: HttpResponse): IO[Unit] = IO {
    println("response.status.code: " + response.status.code)
  }

  implicit val requestId: UUID = UUID.randomUUID()
  request(uri"https://httpbin.org", Method.GET).unsafeRunSync()
}

trait Greeting {
  lazy val greeting: String = "hello"
}
