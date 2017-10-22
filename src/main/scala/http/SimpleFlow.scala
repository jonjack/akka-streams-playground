package http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.util.Try

object SimpleFlow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val pool: Flow[(HttpRequest, Nothing), (Try[HttpResponse], Nothing), NotUsed] = Http().superPool()

}
