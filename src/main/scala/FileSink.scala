import akka.stream._
import akka.stream.scaladsl._
import akka.{NotUsed, Done}
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import java.nio.file.Paths

/*
 * From https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html#stream-quickstart
 */
object FileSink extends App {

  implicit val system = ActorSystem("SaveIntegersToFile")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)


  val testSink: Flow[String, String, NotUsed] =
    Flow[String]
      //.map(s => ByteString(s))

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
