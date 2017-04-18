val akkaStreamVersion = "1.0"
val akkaVersion = "2.4.8"

lazy val commonSettings = Seq(
  organization  := "com.github.ngjiunnjye",
  version := "0.0.1",
  scalaVersion := "2.11.6",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamVersion,
    "org.apache.kafka"  %  "kafka_2.11"                           % "0.10.1.1", 
    "com.h2database"    %  "h2"                                   % "1.4.193",
    "io.spray"          %% "spray-json"                           % "1.3.2",
    "org.scalatest"     %% "scalatest"                            % "2.2.5" % "test",
    "com.typesafe.akka" %% "akka-testkit"                         % akkaVersion % "test"
  ),
  ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

lazy val root = project aggregate(common, redirect)

lazy val common = (project in file("url-shortening-common"))
  .settings(name := """url-shortening-common""")
  .settings(commonSettings: _*)

lazy val redirect = (project in file("url-shortening-redirect"))
  .settings(name := """url-shortening-redirect""")
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val creator = (project in file("url-shortening-creator"))
  .settings(name := """url-shortening-creator""")  
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster"                         % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools"                   % akkaVersion
  ))
  .dependsOn(common, redirect)

fork in run := true
