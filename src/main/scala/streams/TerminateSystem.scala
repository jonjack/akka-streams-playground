package streams

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Future

/**
  * Some examples of shutting down the Actor system.
  */
object TerminateSystem {

  implicit val system = ActorSystem()               // needed to create a Materializer
  implicit val materializer = ActorMaterializer()   // used for materializing a flow
  implicit val ec = system.dispatcher               // used in system.terminate()

  // Create some super simplw flow just for demonstration
  val source = Source(1 to 10)   // emits integers in a range
  val flow = source.map(x => x)  // a silly flow that just passes the element on unchanged
  val sink = Sink.ignore         // a sink that does nothing


  // One way to terminate the system is to

  // Example 1 - calling terminate in a callback
  val future: Future[Done] =
    source
      .toMat(sink)(Keep.right)
      .run()
  future.onComplete(_ => system.terminate())


  // Example 2 - passing terminate callback directly on flow
  source
    .toMat(sink)(Keep.right)
    .run()  // this returns a Future
    .onComplete(_ => system.terminate())

}
