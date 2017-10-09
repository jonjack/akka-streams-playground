import akka.stream._
import akka.stream.scaladsl._
import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import scala.concurrent._

/*
 * From https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html#stream-quickstart
 */
object PrintIntegers extends App {

  //print("Hi")

  implicit val system = ActorSystem("PrintIntegers")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done: Future[Done] = source.runForeach(i => println(i))(materializer)

  // When you start up the ActorSystem it is never terminated until instructed.
  // Luckily runForeach returns a Future[Done] which resolves when the stream finishes.
  // So we can provide a callback to terminate the ActorSystem once the stream has
  // finished being processed.
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())

}
