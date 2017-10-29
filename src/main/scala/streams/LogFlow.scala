package streams

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

/**
  * An example of logging within a flow.
  */
object LogFlow extends App {

  implicit val system = ActorSystem("LogFlow")
  implicit val materializer = ActorMaterializer()

  val log: LoggingAdapter = Logging.getLogger(system, this)

  // will look for logger configured with name "success"
  val logToSuccessLog: LoggingAdapter = Logging.getLogger(system, "success")

  // will look for logger configured with name "failure"
  val logToFailureLog: LoggingAdapter = Logging.getLogger(system, "failure")

  log.info("Sample log entry from LogFlow")
  logToSuccessLog.info("Sample log entry from LogFlow")
  logToFailureLog.info("Sample log entry from LogFlow")

//  val logger: LoggingAdapter = Logging.getLogger(system, ConfigFactory.load())
//  logger.info("LOGGED BY LOGGER")
//  logger.debug("LOGGED BY LOGGER")

  val source: Source[Int, NotUsed] = Source(1 to 10)

  // This flow simply logs the value that flows through it
  // It takes an int, logs it, and then emit the Int downstream unchanged
  val logFlow: Flow[Int, Int, NotUsed] =
    Flow[Int]
      .log("logFlow")
      .map(i => i)

  // A Sink which does nothing since we are just demo-ing the flow logging
  // We need one to create the graph
  val ignorantSink = Sink.ignore

  val graph: Future[Done] = source.via(logFlow).runWith(ignorantSink)

  // When you start up the ActorSystem it is never terminated until instructed.
  // runForeach returns a Future[Done] which resolves when the stream finishes.
  // So we can provide a callback to terminate the ActorSystem once the stream has
  // been completed.
  implicit val ec = system.dispatcher
  graph.onComplete(_ => system.terminate())

}
