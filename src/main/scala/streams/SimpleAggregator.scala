package streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent._

object SimpleAggregator extends App {

  // Get an Actor Factory and Materializer (these will be implicitly used)
  implicit val system = ActorSystem("SimpleAggregator")
  implicit val materializer = ActorMaterializer()

  // We need this to help close down the Actor system onComplete
  implicit val ec = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 10)
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(i => "Number " + i.toString())
  val sink: Sink[String, Future[Seq[String]]] = Sink.seq[String]

  val graph: RunnableGraph[Future[Seq[String]]] = source.via(flow).toMat(sink)(Keep.right)

  // Instead of using above verbose `sink` definition we could just use Sink.seq
  // val graph: RunnableGraph[Future[Seq[String]]] = source.via(flow).toMat(Sink.seq)(Keep.right)

  val sum: Future[Seq[String]] = graph.run()

  // On completion of the stream computation, print result and terminate Actor system
  // The result of the Future is either Success[Seq[String]] or Failure
  // So have to call get() on the result to get the Seq before we can map over it.
  sum.onComplete(xs => {
    xs.get.map(println _)   // sugared form of (_ => print(_))
    system.terminate()
  } )

}