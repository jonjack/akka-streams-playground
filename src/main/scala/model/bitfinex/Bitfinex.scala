package model.bitfinex

import spray.json._

object Bitfinex {

  case class Stat(period: Int, volume: String)
  case class Stats(data: Array[Stat])

  object StatsProtocol {
    import DefaultJsonProtocol._
    implicit val statFormat: JsonFormat[Stat] = jsonFormat2(Stat)
    implicit val statsFormat: JsonFormat[Stats] = jsonFormat1(Stats)
  }

  import DefaultJsonProtocol._
  import StatsProtocol._

  val stat1 = Stat(1, "7967.96766158")
  val stat2 = Stat(7, "55938.67260266")
  val stat3 = Stat(30, "275148.09653645")
  val stats = Stats(Array(stat1, stat2, stat3))
  val statsjson: String = stats.data.toJson.prettyPrint

  val test: Boolean = statsjson == """[{""" + "\n" +
    """  "period": 1,""" + "\n" +
    """  "volume": "7967.96766158"""" + "\n" +
    """}, {""" + "\n" +
    """  "period": 7,""" + "\n" +
    """  "volume": "55938.67260266"""" + "\n" +
    """}, {""" + "\n" +
    """  "period": 30,""" + "\n" +
    """  "volume": "275148.09653645"""" + "\n" +
    """}]"""

}