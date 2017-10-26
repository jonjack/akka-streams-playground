package streams

import java.nio.file.{Path, Paths}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Flow, Framing, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Read lines from a file and emit each line downstream in a throttled fashion.
  */
object FileSourceThrottle extends App {

  implicit val system = ActorSystem("SimpleThrottle")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val path: String = "./customers.txt"
  val file: Path = Paths.get(path)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

  val flow = Flow[ByteString].
    via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).
    throttle(1, 1.second, 1, ThrottleMode.shaping).
    map(bs => bs.utf8String)

  val sink = Sink.foreach(println)

  /*
   * The secret to getting this to work was to use runWith(sink) rather than to(sink).
   */
  val graph: Unit = source.via(flow).runWith(sink).onComplete(_ => system.terminate())

}
