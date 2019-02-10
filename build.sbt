ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol, slack, web)
lazy val slack = (project in file("slack"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.seratch" % "jslack" % "1.1.7",
      "eu.timepit" %% "refined" % refinedVersion,
      "eu.timepit" %% "refined-pureconfig" % refinedVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.10.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.typelevel" %% "kittens" % "1.2.0",
      "io.chrisdavenport" % "cats-time_2.12" % "0.2.0",
      "org.typelevel" %% "cats-effect" % "1.2.0"
    ) ++ commonLibraryDependencies
  )
lazy val seed = (project in file("seed"))
  .settings(
    libraryDependencies += scalaTest
  )
lazy val lol = (project in file("lol"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += scalaTest
  )

lazy val web = (project in file("web"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      scalaTest,
      "com.softwaremill.macwire" %% "macros" % "2.3.1" % Provided,
      "com.dripower" %% "play-circe" % "2711.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-java8" % circeVersion,
      "io.circe" %% "circe-refined" % circeVersion
    ) ++ silencers ++ commonLibraryDependencies
  )
  .enablePlugins(PlayScala)
  .dependsOn(slack)

lazy val circeVersion = "0.11.1"
lazy val refinedVersion = "0.9.4"

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

lazy val silencers = {
  val version = "1.3.0"
  "com.github.ghik" %% "silencer-lib" % version % Provided ::
    compilerPlugin("com.github.ghik" %% "silencer-plugin" % version) :: Nil
}

lazy val commonLibraryDependencies = Seq(
  "com.github.bigwheel" %% "util-backports" % "1.1"
)
lazy val commonSettings = Seq(
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
)
