package json

import spray.json._
import DefaultJsonProtocol._

object SimpleMarshaller extends App {

  case class Customer(
                       bpid: String,
                       title: String,
                       firstname: String,
                       surname: String,
                       brand: String,
                       email: String,
                       channel: String)


  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val colorFormat = jsonFormat7(Customer)
  }

  import MyJsonProtocol._
  import spray.json._

  val json = Customer("30065400124", "Ms", "Tera", "Patrick", "SE", "tera.patrick@hotmail.com", "POT3").toJson
  val customer = json.convertTo[Customer]

  println(json)
  println(customer)

}
