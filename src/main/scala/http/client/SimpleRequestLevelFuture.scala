package http.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object SimpleRequestLevelFuture extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  //TODO`

}
