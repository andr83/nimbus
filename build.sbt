name := "nimbus"

version := "0.2"

scalaVersion := "2.12.4"

enablePlugins(JavaAppPackaging)

dockerUsername := Some("andr83")

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "3.0.0-RC1",
  "com.github.andr83" %% "scalaconfig" % "0.4",
  "com.typesafe" % "config" % "1.3.1",
  "com.softwaremill.sttp" %% "core" % "1.1.5",
  "org.influxdb" % "influxdb-java" % "2.8",
  "org.jsoup" % "jsoup" % "1.11.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

libraryDependencies ++= Seq(
  "io.parsek" %% "parsek-core",
  "io.parsek" %% "parsek-jackson",
  "io.parsek" %% "parsek-shapeless"
).map(_ % "0.2.0")
