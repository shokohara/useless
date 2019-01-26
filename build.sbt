ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol)
lazy val seed = (project in file("seed"))
  .settings(
    libraryDependencies += scalaTest
  )
lazy val lol = (project in file("lol"))
  .settings(
//    fork in run := true,
//    connectInput := true,
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(scalaTest,
      "com.merakianalytics.orianna" % "orianna" % "3.0.4",
      "com.github.taycaldwell" % "riot-api-java" % "4.1.0",
      "org.typelevel" %% "cats-effect" % "1.2.0")
  )

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
