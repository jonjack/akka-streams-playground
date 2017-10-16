package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future

object SimpleBitFuture extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val resp: Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = "https://api.bitfinex.com/v1/symbols"))

  resp.onComplete(hr => {
    print(hr.get.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
      println("Got response, body: " + body.utf8String)
    })
    system.terminate()
  } )

}
