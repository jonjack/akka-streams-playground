package http.client

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.actor.Status.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.Try

object SimpleRequestLevelFlow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  //TODO`

  case class Customer(id: Int, name: String)

  val request: HttpRequest = HttpRequest(uri = "https://api.bitfinex.com/v1/symbols")
  val source: Source[HttpRequest, NotUsed] = Source.single(request)
  val flow: Flow[(HttpRequest, Nothing), (Try[HttpResponse], Nothing), NotUsed] = Http().superPool()

  /*
  source.via(flow).map(t => t).runWith(Sink.foreach(t => Unit))

    .map(t => t.)
    .map(_._1)
    .runWith(Sink.foreach(writeFile(downloadDir)))

*/

  /*
  val poolFlow: Flow[(HttpRequest, Nothing), (Try[HttpResponse], Nothing), NotUsed] = Http().superPool[Unit]()
  poolFlow.prepend(source)
  poolFlow.toMat(Sink.foreach({
    case ((Success(resp), p)) => p. uccess(resp)
    case ((Failure(e), p))    => p.failure(e)
  }))(Keep.left)
    .run()
    */



  //val graph: RunnableGraph[NotUsed] = poolFlow.runWith(source, sink)

  /*
  val httpPool = Http().superPool[Unit]()
  Source
    .single((request, ()))
    .via(httpPool)
    .completionTimeout(requestTimeout.duration)
    .mapAsyncHttpResponse(t => Future.fromTry(t._1))
    .runWith(Sink.head)
  */


}
