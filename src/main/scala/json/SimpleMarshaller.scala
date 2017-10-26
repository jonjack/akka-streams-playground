package json

import spray.json._
import DefaultJsonProtocol._

object SimpleMarshaller extends App {

  case class Data (attributes: Customer)
  case class Customer(
                       title: String,
                       firstName: String,
                       surname: String,
                       brands: String,
                       email: String,
                       channel: String,
                       status: String)


  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val customerFormat = jsonFormat7(Customer)
  }

  import MyJsonProtocol._
  import spray.json._

  /*
  val json = Customer("30065400124", "Ms", "Tera", "Patrick", "SE", "tera.patrick@hotmail.com", "POT3").toJson
  val customer = json.convertTo[Customer]

  println(json)
  println(customer)
  */

}
