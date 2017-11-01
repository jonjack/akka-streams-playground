package http.client

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import streams.LogFlow.system

import scala.concurrent.Future

object TestHostLevelFlow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  //val successSink = FileIO.toPath(Paths.get("logs/success.txt"))
  //val failureSink = FileIO.toPath(Paths.get("logs/failure.txt"))

  //val logSuccess: LoggingAdapter = Logging.getLogger()
  val log: LoggingAdapter = Logging.getLogger(system, this)
  val logsuccess: LoggingAdapter = Logging.getLogger(system, "success")
  val logfail: LoggingAdapter = Logging.getLogger(system, "failure")


  val conn = Http().cachedHostConnectionPoolHttps[String]("api.bitfinex.com", 443)
  val symbols = List("btcusd", "ethusd", "omgusd", "xxxusd")
  val fut: Future[Map[String, HttpResponse]] =
    Source(symbols).
      map(symbol => (HttpRequest(GET, s"/v1/stats/$symbol"), symbol)).
      via(conn).
      runFold(Map.empty[String, HttpResponse]) {
        case (map, (util.Success(resp), symbol)) => {
          resp.status.intValue match {
            //case 200 => print("OK 200 [" + symbol + "]")
            //case _ => print("NOT OK [" + symbol + "]")
            case 200 => logsuccess.info("OK 200 [" + symbol + "]")
            case status => logfail.info("BAD " + status + " [" + symbol + "]")
          }
          map ++ Map(symbol -> resp)}
        case (map, (util.Failure(ex), symbol)) => {print("FAILURE");map}
      }

  fut.onComplete(hr => {
    hr.get.map(tup => {println("RESPONSE: REQUEST_ID[" + tup._1 + "]  CONTENT[" + tup._2.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
      println("Got response, body: " + body.utf8String)
    }) + "]"})
    system.terminate()
  } )

}
