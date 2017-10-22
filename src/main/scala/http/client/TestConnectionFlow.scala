package http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object TestConnectionFlow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val request = HttpRequest(GET, "/v1/stats/btcusd")

  //val connectionFlow = Http().outgoingConnection("api.bitfinex.com", 443) // plain HTTP connection to port 80
  val connectionFlow = Http().outgoingConnectionHttps("api.bitfinex.com", 443)

  Source.single(request).
    via(connectionFlow).
    flatMapConcat(_.entity.dataBytes).
    //via(Framing.delimiter(ByteString(","), 1024)). // will split each line of response using comma (,) as delimeter
    map(x => println("JSON RESPONSE: " + x.utf8String)).
    //mapAsyncUnordered(16)(print).
    runWith(Sink.ignore)
    .onComplete(_ => system.terminate())

}
