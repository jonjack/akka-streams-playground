import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ThrottleMode}

import scala.concurrent._
import scala.concurrent.duration._

object SimpleThrottle extends App {

  implicit val system = ActorSystem("SimpleThrottle")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 1000000000)   // lets process 1 billion integers!

  //val done: Future[Done] = source.runForeach(i => println(i))(materializer)
  //val done: Future[Done] = source.runForeach(i => println(i))   // materializer passed implicitly
  //val done: Future[Done] = source.runForeach(println)    // sugared function

  val done: Future[Done] = source.throttle(1, 1.second, 1, ThrottleMode.shaping).runForeach(println)

  // utility to close down the ActorSystem when the stream has completed
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
