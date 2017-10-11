
# Learning Akka Streams


## Core Concepts

Akka streams is a library for processing sequences of elements using bounded buffer space. You can create _sequences_ or _graphs_ of processing steps (entities) which will execute independently (and possibly concurrently) of each other while buffering a limited number of elements at any point in time (in order to ensure that producers do not overwhelm consumers). The _boundedness_ (of processing buffers) is fundamental to Akka streams since they are designed for scenarios where you need to ensure you process all elements and cannot afford to skip (drop) any. This differs from the actor model where mailboxes can be bounded and elements may be dropped.

Refer to the docs for a brief glossary of the [main concepts](https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-flows-and-basics.html#core-concepts).


## Fundamentals

**Source** (input)         
A processing stage that has one output, emitting data elements whenever downstream stages are ready to receive them eg. a source might read/parse an input file and emit each line (as a data element) downstream.

**Sink** (output)    
A processing stage with one input, requesting and accepting data elements, and possible slowing down the upstream producer if it has no room in it's buffer (using _backpressure_), eg. a Sink may take strings and write them into a log file.

**Flow** (processing stage)         
A processing stage which connects one input and one output and usually transform the data flowing through it. Note that a Flow may not be directly connected to a Source (upstream) or Sink (downstream), as the following examples demonstrate. 

```text
+--------+       +--------+      +--------+
| Source |------>|  Flow  |----->|  Sink  |
+--------+       +--------+      +--------+

+--------+       +--------+      +--------+      +--------+
| Source |------>|  Flow  |----->|  Flow  |----->|  Sink  |
+--------+       +--------+      +--------+      +--------+

+--------+       +--------+      +--------+       +--------+      +--------+
| Source |------>|  Flow  |----->|  Flow  |------>|  Flow  |----->|  Sink  |
+--------+       +--------+      +--------+       +--------+      +--------+
```

Note that the [documentation](https://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-flows-and-basics.html#defining-and-running-streams) seems to use the term _flow_ to refer to both a single processing stage, and also a series of connected stages.

**RunnableGraph[T]** (aka _Graph_, _Flow_, _Pipeline_, _Stream_)    
A Flow that has both ends attached to a `Source` and a `Sink`. It is the runnable entity in a stream and is executed by invoking it's `run()` method - this triggers the process referred to as _materialization_ and no data flows through the stream until this occurs. 

The [T] represents the type of the final result of completing computation of the graph. Materializing the following example would produce a `Future[Int]` as a result. 

```scala
val runnable: RunnableGraph[Future[Int]] = ...

// materialize the flow and get the value of the graph - Future[Int]
val result: Future[Int] = runnable.run()

// we pass a callback to the Future to simply print the result when done
result.onComplete(_ => print(_))
```

**Graphs are a Blueprint**    
Graphs just describe a Stream - they are a blueprint for an execution plan. The individual components (`Source`, `Flow`, `Sink`) of the graph are just blueprints themselves for their individual respective tasks and can therefore be reused in other graphs. Graphs themselves may be incorporated into larger graphs.

---

> #### _Terms used to describe an end-to-end stream_
> It seems (from my interpretation of reading the docs) that all the following terms may be used to describe a runnable end-to-end stream that has an input, output and 1 or more processing stages:-
>
> _Stream_    
  _Pipeline_    
  _Flow_    
  _RunnableGraph_    
  _Graph_    

---

**Materialization**         
This is the process of actually allocating the resources required to run the computation described by a Graph. In Akka Streams this generally involves starting up Actors, which power the processing, but can also mean opening files or socket connections etc, depending on what the stream requires.

See [the docs](https://doc.akka.io/docs/akka/current/scala/stream/stream-flows-and-basics.html#stream-materialization) for more.


**Materializer**
The Materializer is a factory for stream execution engines, it is the thing that makes streams run. Methods like [RunnableGraph.run()](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Flow.scala#L496) require an implicit Materlializer to be in scope.

If you look at the signature of the factory constructor for [ActorMaterializer](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/ActorMaterializer.scala#L39), you will see that it requires an implicit [ActorRefFactory](https://github.com/akka/akka/blob/master/akka-actor/src/main/scala/akka/actor/ActorRefProvider.scala#L189) to be in scope in order to generate Actors. The type of factory we use is an [ActorSystem](https://github.com/akka/akka/blob/master/akka-actor/src/main/scala/akka/actor/ActorSystem.scala#L135).

So we need a Materializer, and that, in turn, requires an ActorSystem.

```scala
implicit val system = ActorSystem("SimpleGraph")
implicit val materializer = ActorMaterializer()
```

**Materialized Value**    
This is the computed result (and it's type) of a stage.   
When we run (_materialize_) a Graph, each processing stage (whether it be a _Source_, _Flow_ or _Sink_) produces a result which is the input to a downstream stage (unless it is the sink at the end of the graph, in which case it is the final output). Each of these results is what we call the _materialized value_ of that stage. 


---

## Source

Here is the signature of [Source](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Source.scala#L32).

```scala
final class Source[+Out, +Mat]
```

The Source type is parameterized with two types: the first one (`+Out`) is the type of element that this source emits and the second one (`+Mat`) may signal that running the source produces some auxiliary value (e.g. a network source may provide information about the port it is bound to). Where no auxiliary information is produced, the type `akka.NotUsed` is used.

The following source is a simple range of integers. It emits integers `Int` but produces no auxiliary information, hence the second type argument is `NotUsed`

```scala
val source: Source[Int, NotUsed] = Source(1 to 100)
```

## Flow

Here is the signature of [Flow](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Flow.scala#L27).

```scala
final class Flow[-In, +Out, +Mat]
```


## Sink

Here is the signature of [Sink](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Sink.scala#L26).

```scala
final class Sink[-In, +Mat]
```

The first type parameter (a contravariant `-In`) is the type that the Sing consumes. 




---

## Understanding Materialized Values



To try and understand how materialized values work we will use a simple example below that has a simple Source of a few integers. A flow which just transforms the data by filtering out the odd numbers in the range. A Sink which does not produce anything, it just has a side effect of printing out each element it consumes.

```scala
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent._

object SimpleGraph extends App {

  implicit val system = ActorSystem("SimpleGraph")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 10)
  val flow: Source[Int, NotUsed] = source.filter(i => i % 2 == 0) // filter out odd numbers

  val sink: Sink[Int, Future[Done]] = Sink.foreach(i => print(i))
  
  val graph: RunnableGraph[NotUsed] = flow.to(sink)
  val materialize: NotUsed = graph.run()

}

// this just prints: 246810
```

How do the materialized values work in the above and then we need to ue different versions to get different results of materialized values.



## Source

Here is simple source of integers in the range 1->100.

```scala
val source: Source[Int, NotUsed] = Source(1 to 100)
```

The first type parameter `Int` is the type of the element that this type emits. The second type parameter is used to signal that the source produces some auxiliary information (eg. a network source may provide the port that it is bound to). Where the source provides no auxiliary information, the type `akka.NotUsed` is used (as in this case).


## Throttling Processing Speed

See [Time-Based Processing](https://doc.akka.io/docs/akka/current/scala/stream/stream-quickstart.html#time-based-processing).

We can impose a time constraint on a stream such it can only flow at a certain speed ie. we slow it down using the `throttle` combinator.

The following stream processes 1 billion integers and just writes them out to the system. Handling such a large source did not cause any memory problems when running on my Mac. After about 15 seconds of running this graph roughly 5 million integers had been printed. Thats about 300,000 integers printed per second (on my Mac without much else running). Running VisualVM at the same time showed that CPU was hardly used and neither was memory.

```scala
import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl._

import scala.concurrent._

object SimpleThrottle extends App {

  implicit val system = ActorSystem("SimpleThrottle")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 1000000000)   // lets process 1 billion integers!

  val done: Future[Done] = source.runForeach(i => println(i))(materializer)

  // Note the following would work as well
  //val done: Future[Done] = source.runForeach(i => println(i))   // materializer passed implicitly
  //val done: Future[Done] = source.runForeach(println)    		  // sugared function

  // shut down the ActorSystem when the stream has completed
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
```

We could throttle the above stream so that it only handled 1 integer per second.

```scala
import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl._

import scala.concurrent._
import scala.concurrent.duration._

object SimpleThrottle extends App {

  implicit val system = ActorSystem("SimpleThrottle")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 1000000000)   // lets process 1 billion integers!

  // We add a throttle 
  val done: Future[Done] = source.throttle(1, 1.second, 1, ThrottleMode.shaping).runForeach(println)

  // shut down the ActorSystem when the stream has completed
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
```

The secret that makes the above work is that Akka Streams implicitly implement pervasive flow control, all combinators respect back-pressure. This allows the throttle combinator to signal to its upstream source of data that it can only accept elements at a certain rate. When the incoming rate is higher than one per second, the throttle combinator will assert _back-pressure_ upstream. In our example, this causes the `source` to emit elements at a rate of just one per second, rather than about 300,000 per second as we saw in the first example!


