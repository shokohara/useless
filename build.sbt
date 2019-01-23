ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol)
lazy val seed = (project in file("seed"))
  .settings(
    libraryDependencies += scalaTest
  )
lazy val lol = (project in file("lol"))
  .settings(
    libraryDependencies += scalaTest
  )

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
