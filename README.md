
# Learning Akka Streams

[Streams Quickstart Guide](https://doc.akka.io/docs/akka/current/scala/stream/stream-quickstart.html)


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

## The 3 components of a Stream

Streams always start flowing from a `Source[Out,M1]` then can continue through `Flow[In,Out,M2]` elements or more advanced graph elements to finally be consumed by a `Sink[In,M3]`


### Source

Built-in [Sources](https://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#source-stages).

An instance of the type `Source[Out]` produces a potentially unbounded stream of elements of type `Out`.

Here is the actual signature of [Source](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Source.scala#L32).

```scala
final class Source[+Out, +Mat]
```

Type parameters:-     
`+Out` - the type of element it produces (_emits_, _outputs_).      
`+Mat` - the type of the Source's _materialized value_ (auxiliary data).

> _**auxiliary**_ definition.
> Providing supplementary or additional help.

The _**materialized value**_ (the type of which is specified using the `Mat` type parameter) is an additional (_auxiliary) element that the Source might produce, in addition to the `Out` element that it emits. This value might be useful (depending on the scenario) for further computation, an example being a network source may provide information about the port it is bound to. 

Where no auxiliary information is produced, the type `akka.NotUsed` is used.

The following source is a simple range of integers. It emits integers `Int` but produces no auxiliary information, hence the second type argument is `NotUsed`

```scala
val source: Source[Int, NotUsed] = Source(1 to 100)
```

### Flow

Built-in [Flows](https://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#flow-stages).

Here is the signature of [Flow](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Flow.scala#L27).

```scala
final class Flow[-In, +Out, +Mat]
```

Type parameters:-    
 `-In` - the type of element it consumes.      
 `+Out` - the type of element it produces (_emits_, _outputs_).     
 `+Mat` - the type of the Flow's _materialized_ value.    


### Sink

Built-in [Sinks](https://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#sink-stages).

Here is the signature of [Sink](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/scaladsl/Sink.scala#L26).

```scala
final class Sink[-In, +Mat]
```

Type parameters:-     
`-In` - the type that the Sink accepts as input (_consumes_).       
`+Mat` - the type of the Sink's _materialized_ value.

Some examples of Sinks that produce materialized values on completion:-

- _ForeachSinks_ produce a `Future[Done]` that completes when the stream completes. 
- _FoldSinks_, which fold some number of elements of type `A` into an initial value of type `B` using a function `(A, B) => B`, produces a `Future[B]` that completes when the stream completes.


---

## Understanding Materialized Values


See [Materialized values](https://doc.akka.io/docs/akka/current/scala/stream/stream-quickstart.html#materialized-values)

Materialized values (those `Mat` types) may be produced by one or more of the stages in a stream. 

Streams will flow like this:-

```scala
Source[Out] ~~~> Flow[In,Out] ~~~> Sink[In]
```

Consider this example. Imagine a stream which starts with a `Source[Int]` that reads from a File and emits a stream of Integers representing customer IDs in a system. This stream of Int's then flows into a transformation stage `Flow[Int,String]` which calls some API to fetch each customer's email address in the system and then emits a stream of Strings. The stream of Strings (email addresses) finally flows into a Sink which simply aggregates them into a `List[String]`. It is this List that we are interested in, so how do we get it?

Here is the example flow in code.

```scala
val source: Source[Int, NotUsed] = Source(1 to 10)
val flow: Flow[Int, String, NotUsed] = Flow[Int].map(i => "Number " + i.toString())
val sink: Sink[String, Future[Seq[String]]] = Sink.seq[String]

val graph: RunnableGraph[Future[Seq[String]]] = source.via(flow).toMat(sink)(Keep.right)
val result: Future[Seq[String]] = graph.run()

result.onComplete( seq => // do something useful with that Seq of emails )
```

Notice the definition of the Sink. It takes a `String` in, and it _materializes_ (produces) a `Future[Seq[String]]`

```scala
Sink[In, Mat]                         // the trait
Sink[String, Future[Seq[String]]]     // our concrete instance
```

It is the `Seq[String]` that we want to do something with once the computation has ended, hence we provide a callback to the Future with.

```scala
result.onComplete( seq => // do something useful with that Seq of emails )
```

This should demonstrate why Materialized values are important. In the above example, access to the materialized value was actually critical to getting our job done since we needed to get hold of that `Seq[String]` when it was complete, or else have sent an alert or something if it has failed. Sometimes we are not actually interested in the result because we will probably have sent it elsewhere but we will still generally always want to know if it succeeded or not and report on what happened ie. get some metrics about how much data was processed etc.


Read the following for more insight:-

[Combining materialized values](https://doc.akka.io/docs/akka/snapshot/scala/stream/stream-flows-and-basics.html#combining-materialized-values)    
[Reusable Pieces](https://doc.akka.io/docs/akka/snapshot/scala/stream/stream-quickstart.html#reusable-pieces)     
[Defining and running streams](https://doc.akka.io/docs/akka/current/scala/stream/stream-flows-and-basics.html#defining-and-running-streams)    


#### Keep.left, Keep.both, Keep.right

Every stream processing stage can produce a materialized value, and it is the responsibility of the user to combine them to a new type.

In th above example, if we do not use the convenience function `(Keep.right)` to indicate that we are only interested in the materialized value of the `Sink` we get the following type for the graph which is produced via a combination of all the materialized values being combined.

```scala
val graph: ((NotUsed, Future[Seq[String]]) => Nothing) => RunnableGraph[Nothing] = source.via(flow).toMat(sink)
```

There are convenience functions (`Keep.left`, `Keep.both`, `Keep.right`) and methods which allow us to produce the materialized types we are interested in. I think the default behaviour is `Keep.left`.

See [Combining materialized values](https://doc.akka.io/docs/akka/snapshot/scala/stream/stream-flows-and-basics.html#combining-materialized-values).


---

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


