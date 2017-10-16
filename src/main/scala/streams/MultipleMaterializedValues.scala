package streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent._

object MultipleMaterializedValues extends App {

  implicit val system = ActorSystem("MultipleMaterializedValues")
  implicit val materializer = ActorMaterializer()

  // connect the Source to the Sink, obtaining a RunnableGraph
  val sink = Sink.fold[Int, Int](0)(_ + _)

  val runnable: RunnableGraph[Future[Int]] =
    Source(1 to 10).toMat(sink)(Keep.right)

  // get the materialized value of the FoldSink
  val sum1: Future[Int] = runnable.run()
  val sum2: Future[Int] = runnable.run()

  // Terminates ActorSystem when processing has completed.
  implicit val ec = system.dispatcher

  // sum1 and sum2 are different Futures

  sum1.onComplete(i => println("sum1 Future " + sum1.hashCode + " in Thread: " +
    Thread.currentThread().getId() + " yields result = " + i))

  sum2.onComplete(i => println("sum2 Future " + sum2.hashCode + " in Thread: " +
    Thread.currentThread().getId() + " yields result = " + i))
}
