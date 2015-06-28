name := """akka-sonos"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-M2",
  "com.typesafe.akka" %% "akka-testkit" % "2.4-M2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test")
