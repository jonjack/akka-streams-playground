package http.client.bitfinex

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import spray.json._

import scala.concurrent.Future
import scala.util.Try

/**
  * This example uses a Akka Http Host connection to call a Bitfinex public API
  * endpoint (/stats) to get some trading volume metrics for a couple of crypto
  * coin trading pairs. It logs the success and failure cases.
  *
  * Successful cases - whenever we get a Response it is a Success - even if the content is not as we expect
  * Failure cases - when we do not get a Response is a Failure
  *
  * We also map over the Successful cases and split them between those with a Status code OK 200 and
  * those that had a different status.
  */
object HostFlowBitfinexExample extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  case class Stat(period: Int, volume: String) {
    override def toString = "\n" + "period: " + period + "\n" + "volume:" + volume
  }
  case class Stats(data: Array[Stat])

  object StatsProtocol {
    import DefaultJsonProtocol._
    implicit val statFormat: JsonFormat[Stat] = jsonFormat2(Stat)
    implicit val statsFormat: JsonFormat[Stats] = jsonFormat1(Stats)
  }

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val statFormat = jsonFormat2(Stat)
  }

  val log: LoggingAdapter = Logging.getLogger(system, this)
  val logsuccess: LoggingAdapter = Logging.getLogger(system, "success")
  val logfailure: LoggingAdapter = Logging.getLogger(system, "failure")

  val conn = Http().cachedHostConnectionPoolHttps[String]("api.bitfinex.com", 443)
  val symbols = List("btcusd", "ethusd", "omgusd", "xxxusd")  // xxxusd tests the non-200 Response Status case
  val fut: Future[Map[String, HttpResponse]] =
    Source(symbols).
      map(symbol => (HttpRequest(GET, s"/v1/stats/$symbol"), symbol)). // we are using the coin symbol as the request ID
      via(conn).
      runFold(Map.empty[String, HttpResponse]) {

        // This case drills down into successful Responses to log based on whether we got a HTTP Status code 200 or not
        case (map, (util.Success(resp), symbol)) => {
          resp.status.intValue match {
            case 200 => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logsuccess.info("Status OK 200 [" + symbol + "] BODY[" + body.utf8String + "]")
            }
            case status => resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              logfailure.info("Status " + status + " [" + symbol + "] BODY[" + body.utf8String + "]")
            }
          }
          map ++ Map(symbol -> resp)}

        // This case catches the situations where we did not get a response back for some reason.
        case (map, (util.Failure(ex), symbol)) => {
          logfailure.info("Exception " + ex.getMessage)
          map
        }
      }

  fut.onComplete((hr: Try[Map[String, HttpResponse]]) => {
    val respMap: Map[String, HttpResponse] = hr.get
    val total: Int = respMap.size
    val successes: Int = respMap.filter(x => x._2.status.intValue() == 200).size
    val failures: Int = respMap.filter(x => x._2.status.intValue() != 200).size
    println("\r\n/------------ Analytics Start ------------/\r\n")
    println(" Finished run of " + respMap.size + " elements")
    println(" Successes  = " + successes)
    println(" Failures   = " + failures)
    println(" Mismatches = " + (total - (successes + failures)) + " (Network Errors?)")
    println("\r\n/------------ Analytics End ------------/\r\n")

    system.terminate()
  })

}
