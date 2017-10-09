import akka.stream._
import akka.stream.scaladsl._
import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import java.nio.file.Paths

/*
 * From https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html#stream-quickstart
 */
object SaveIntegersToFile extends App {

  implicit val system = ActorSystem("SaveIntegersToFile")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val result: Future[IOResult] =
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  // Terminates ActorSystem when processing has completed.
  implicit val ec = system.dispatcher
  result.onComplete(_ => system.terminate())  // callback terminates system

}

