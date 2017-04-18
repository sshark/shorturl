val akkaStreamVersion = "10.0.5"
val akkaVersion = "2.5.0"

lazy val commonSettings = Seq(
  organization := "com.github.ngjiunnjye",
  version := "0.0.1",
  scalaVersion := "2.12.2",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaStreamVersion,
    "org.apache.kafka" %% "kafka" % "0.10.1.1",
    "com.h2database" % "h2" % "1.4.193",
    "io.spray" %% "spray-json" % "1.3.2",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  ),
  ivyScala := ivyScala.value map {
    _.copy(overrideScalaVersion = true)
  }
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

lazy val root = project aggregate(common, redirect, creator)

lazy val common = (project in file("url-shortening-common"))
  .settings(commonSettings: _*)

lazy val redirect = (project in file("url-shortening-redirect"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val creator = (project in file("url-shortening-creator"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
  ))
  .dependsOn(common, redirect)

fork in run := true
