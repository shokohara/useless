ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol, slack)
lazy val slack = (project in file("slack"))
  .settings(
    libraryDependencies ++= Seq(
      "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.5",
      "com.github.pureconfig" %% "pureconfig" % "0.10.1",
    "org.typelevel" %% "cats-effect" % "1.2.0")
  )
lazy val seed = (project in file("seed"))
  .settings(
    libraryDependencies += scalaTest
  )
lazy val lol = (project in file("lol"))
  .settings(
    libraryDependencies += scalaTest
  )

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
