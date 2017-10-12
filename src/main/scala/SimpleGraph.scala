import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent._

object SimpleGraph extends App {

  implicit val system = ActorSystem("SimpleGraph")
  implicit val materializer = ActorMaterializer()

  // A Source of integers from 1 to 10
  // This source is of type Source(NotUsed, Int]
  val source = Source(1 to 10)

  val simpleFlow = source.filter(i => i % 2 == 0)

  /* Folds over a stream.
     Produces a Sink of type Sink[Int, Future[Int]]
     The provided function is applied to each received element with the output
     of the last evaluation as input.
     The Future[Int] will be completed with the value of the final
     function evaluation when the input stream ends (or failure).
  */
  val sink = Sink.fold[Int, Int](0)(_ + _)

  // connect the Source to the Sink, obtaining a RunnableGraph
  val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

  // materialize the flow and get the value of the FoldSink
  val sum: Future[Int] = runnable.run()

  // Terminates ActorSystem when processing has completed.
  implicit val ec = system.dispatcher
  sum.onComplete(_.map(i =>
    { print("Result of folding 0 to 10 = " + i)
      system.terminate()
    }
    ))

}
