package streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

object LogFlow extends App {

  implicit val system = ActorSystem("LogFlow", ConfigFactory.load.getConfig("akka"))
  implicit val materializer = ActorMaterializer()

  val log = Logging.getLogger(system, this)

  log.debug("TESTD")
  log.info("TESTI")

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done: Future[Done] =
    source
      .log("Before Run")
      .runForeach(i => println(i))(materializer)

  // When you start up the ActorSystem it is never terminated until instructed.
  // Luckily runForeach returns a Future[Done] which resolves when the stream finishes.
  // So we can provide a callback to terminate the ActorSystem once the stream has
  // finished being processed.
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())

}
