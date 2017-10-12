import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl._

object HelloWorld extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("HelloWorld")
  implicit val materializer: Materializer = ActorMaterializer()

  val helloWorldStream: RunnableGraph[NotUsed] =
    Source.single("Hello world")
      .via(Flow[String].map(s => s.toUpperCase()))
      .to(Sink.foreach(println))

  helloWorldStream.run()

  /* Same as above but with sugared flow */
  val helloWorldStreamSuggared: RunnableGraph[NotUsed] =
    Source.single("Hello world")
      .map(s => s.toUpperCase())
      .to(Sink.foreach(println))

  helloWorldStreamSuggared.run()
}
