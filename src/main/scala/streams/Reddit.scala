package streams
//import org.json4s.JsonAST.{JString, JValue}

import scala.collection.immutable._



object Reddit extends App {

  val linksToFetch = 15
  val subredditsToFetch = 5
  val commentsToFetch = 2000
  val commentDepth = 25

  val useragent = Map("User-Agent" -> "wordcloud mcgee")

//  val subs = url(s"http://www.reddit.com/subreddits/popular.json").GET

  //println(pop)

/*
  val page = url(s"http://www.reddit.com/subreddits/popular.json").GET <<? Map("limit" -> subredditsToFetch.toString) <:< useragent

  println(page)

  val fut: Future[List[String]] = Http.default(page OK dispatch.as.json4s.Json).map { json =>
    json.\("data").\("children").children
      .map(_.\("data").\("url"))
      .collect{ case JString(url) => url.substring(3, url.length - 1) }
  }

  val pops: Future[json4s.JValue] = Http.default(subs OK dispatch.as.json4s.Json)

  pops.onComplete(xs => print(xs))

  pops
  */

}
