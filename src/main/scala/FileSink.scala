import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._

/*
 * From https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html#stream-quickstart
 */
object FileSink extends App {

  implicit val system = ActorSystem("SaveIntegersToFile")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val flow: Source[ByteString, NotUsed] =
    source.map(s => ByteString(s))

  val flow1: Flow[String, ByteString, NotUsed] =
    Flow[String].map(s => ByteString(s))

  val flow2: Flow[Int, String, NotUsed] =
    Flow[Int].map(s => s.toString)

  /*
   * This demonstrates a reusable Sink that we can use to write
   * Strings to a file (as ByteStrings).
   */
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val result: Future[IOResult] =
    factorials
      .map(_.toString)
      .runWith(lineSink("factorial2.txt"))

  // Terminates ActorSystem when processing has completed.
  implicit val ec = system.dispatcher
  result.onComplete(_ => system.terminate()) // callback terminates system

}
