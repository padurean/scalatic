import com.github.retronym.SbtOneJar._


name := """scalatic"""

mainClass in Compile := Some("Scalatic")

scalaVersion := "2.11.8"

version := "0.1.0"

oneJarSettings

libraryDependencies ++= List(
  "com.github.nscala-time" % "nscala-time_2.11" % "2.12.0",
  "org.scalaj" % "scalaj-http_2.11" % "2.3.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test")

