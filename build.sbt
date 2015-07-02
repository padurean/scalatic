import com.github.retronym.SbtOneJar._


name := """scalatic"""

mainClass in Compile := Some("Scalatic")

scalaVersion := "2.11.6"

version := "0.1.0"

oneJarSettings

libraryDependencies ++= List(
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test")

