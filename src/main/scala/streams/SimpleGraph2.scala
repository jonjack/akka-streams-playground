package streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._

object SimpleGraph2 extends App {

  implicit val system = ActorSystem("SimpleGraph2")
  implicit val materializer = ActorMaterializer

  val source: Source[Int, NotUsed] = Source(1 to 10)
  val flow: Source[Int, NotUsed] = source.filter(i => i % 2 == 0) // filter out odd numbers

  val sink: Sink[Int, Future[Done]] = Sink.foreach(i => print(i))

  val graph: RunnableGraph[NotUsed] = flow.to(sink)
  //val materialize: NotUsed = graph.run()

  //val graph: RunnableGraph[(NotUsed, Future[Done])] = flow.toMat(sink)(Keep.right)

  //val materialize: (NotUsed, Future[Done]):  = graph.run()

  //val materialize: NotUsed = graph.run()


  //val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  //val graph: RunnableGraph[NotUsed] = flow.to(sink)

  //val graph: ((NotUsed, Future[Int]) => Nothing) => RunnableGraph[Nothing] = flow.toMat(sink)
  //val graph: RunnableGraph[Future[Int]] = flow.toMat(sink)(Keep.right)

  //val runnable: Future[Int] = flow.runWith(sink)

}
