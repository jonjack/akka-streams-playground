package streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

object LogFlow extends App {

  implicit val system = ActorSystem("LogFlow")
  implicit val materializer = ActorMaterializer()

  val log: LoggingAdapter = Logging.getLogger(system, this)
  log.debug("LOGGED BY LOG")
  log.info("LOGGED BY LOG")

  val logger: LoggingAdapter = Logging.getLogger(system, ConfigFactory.load())
  logger.info("LOGGED BY LOGGER")
  logger.debug("LOGGED BY LOGGER")

  val source: Source[Int, NotUsed] = Source(1 to 5)

  val done: Future[Done] =
    source
      .log("hi")
      .runForeach(i => println(i))(materializer)

  // When you start up the ActorSystem it is never terminated until instructed.
  // Luckily runForeach returns a Future[Done] which resolves when the stream finishes.
  // So we can provide a callback to terminate the ActorSystem once the stream has
  // finished being processed.
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())

}
