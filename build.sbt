name := "akka-streams-playground"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.3"

libraryDependencies ++= {
  lazy val akkaVersion        = "2.5.6"
  lazy val akkaHttpVersion    = "10.0.10"
  lazy val scalaTestVersion   = "3.0.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit"         % akkaVersion,
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion,
    "org.scalatest"     %% "scalatest"            % scalaTestVersion    % "test",
    "com.typesafe.akka" %% "akka-slf4j"           % "2.5.6",
    "ch.qos.logback"    %  "logback-classic"      % "1.2.3"
  )
}

resolvers += "SBT Releases" at "https://dl.bintray.com/sbt/sbt-plugin-releases/"

enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("streams.SimpleGraph")

//"net.databinder.dispatch" %% "dispatch-core" % "0.13.2",
//"net.databinder.dispatch" %% "dispatch-json4s-native" % "0.13.2"
