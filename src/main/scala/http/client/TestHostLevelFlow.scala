package http.client

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.actor.Status.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import http.SimpleBitFuture.{resp, system}

import scala.concurrent.Future

object TestHostLevelFlow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val conn = Http().cachedHostConnectionPoolHttps[String]("api.bitfinex.com", 443)
  val symbols = List("btcusd", "ethusd", "omgusd")
  val fut: Future[Map[String, HttpResponse]] =
    Source(symbols).
      map(symbol => (HttpRequest(GET, s"/v1/stats/$symbol"), symbol)).
      via(conn).
      runFold(Map.empty[String, HttpResponse]){
        case (map, (util.Success(resp), symbol)) =>
          map ++ Map(symbol -> resp)
        case (map, (util.Failure(ex), symbol)) => map
      }

  fut.onComplete(hr => {
    hr.get.map(tup => {println("XXX: " + tup._1 + tup._2);tup})
    system.terminate()
  } )

}
