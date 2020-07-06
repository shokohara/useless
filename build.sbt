ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol, slack, web, minsoku)

lazy val slack = project
  .settings(commonSettings)
  .settings(
    mUnitSettings,
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "org.typelevel" %% "cats-core" % "2.1.1",
      "com.github.seratch" % "jslack" % "3.4.2",
      "jp.t2v" %% "holidays" % "6.0",
      "eu.timepit" %% "refined" % refinedVersion,
      "eu.timepit" %% "refined-pureconfig" % refinedVersion,
      "com.github.pureconfig" %% "pureconfig-generic" % pureconfigVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.typelevel" %% "kittens" % "2.1.0",
      "io.chrisdavenport" %% "cats-time" % "0.3.0",
      "org.typelevel" %% "cats-effect" % "2.1.3",
      "com.lihaoyi" %% "sourcecode" % "0.2.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )

lazy val seed = project.settings(
  libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files" % "3.9.1"
  )
)

lazy val minsoku = project.settings(
  mUnitSettings,
  fork := true,
  libraryDependencies += "org.scalameta" %% "munit" % "0.7.9" % Test,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.1.1",
    "org.seleniumhq.selenium" % "selenium-java" % "4.0.0-alpha-6",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.typelevel" %% "cats-effect" % "2.1.3",
    "org.spire-math" %% "antimirov-core" % "0.2.4",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "io.chrisdavenport" %% "cormorant-core" % "0.3.0",
    "io.chrisdavenport" %% "cormorant-generic" % "0.3.0"
  )
)
lazy val lol = project.settings(commonSettings)

lazy val web = project
  .settings(commonSettings)
  .settings(
    mUnitSettings,
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "com.lihaoyi" %% "sourcecode" % "0.2.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "eu.timepit" %% "refined" % refinedVersion,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "be.venneborg" %% "play27-refined" % "0.5.0",
      "com.softwaremill.macwire" %% "macros" % "2.3.7" % Provided,
      "com.dripower" %% "play-circe" % "2812.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-refined" % circeVersion,
      "com.typesafe.play" %% "play-json" % "2.7.1", // 一時的にsbt-play-swaggerのために必要
      "org.webjars" % "swagger-ui" % "2.2.0"
    ),
    libraryDependencies --= Seq(
      "com.typesafe.play" %% "filters-helpers" % "2.7.0",
      "com.typesafe.play" %% "play-akka-http-server" % "2.7.0",
      "com.typesafe.play" %% "play-json" % "2.7.1",
      "com.typesafe.play" %% "play-logback" % "2.7.0",
      "com.typesafe.play" %% "play-server" % "2.7.0",
      "org.webjars" % "swagger-ui" % "2.2.0"
    ),
    unusedCompileDependenciesFilter := moduleFilter(),
    buildInfoKeys := Seq[BuildInfoKey](
      "gitHeadCommit" -> git.gitHeadCommit.value.getOrElse(""),
      "gitHeadCommitDate" -> git.gitHeadCommitDate.value.getOrElse(""),
      "gitHeadMessage" -> git.gitHeadMessage.value.getOrElse(""),
      "gitBranch" -> git.gitCurrentBranch.value
    ),
    buildInfoOptions += BuildInfoOption.ToMap,
    routesImport ++= Seq(
      "be.venneborg.refined.play.RefinedPathBinders._",
      "be.venneborg.refined.play.RefinedQueryBinders._",
      "com.github.shokohara.playextra.QueryStringBindable._",
      "eu.timepit.refined.types.string._"
    ),
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    wartremoverWarnings in (Compile, compile) := Seq.empty,
    dockerBaseImage := "openjdk:8u181-jdk-stretch",
    daemonUser in Docker := "root",
    dockerEntrypoint := Seq("/bin/sh", "-c"),
    dockerCmd := "/opt/docker/bin/web"
      :: "-XX:+UnlockExperimentalVMOptions"
      :: "-XX:+UseCGroupMemoryLimitForHeap"
      :: Nil
  )
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .dependsOn(slack)

lazy val circeVersion = "0.13.0"
lazy val refinedVersion = "0.9.14"
lazy val pureconfigVersion = "0.12.3"
lazy val doobieVersion = "0.6.0"

lazy val mUnitSettings = Seq(
  libraryDependencies += "org.scalameta" %% "munit" % "0.7.9" % Test,
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-Xfatal-warnings", "-deprecation", "-feature", "-language:higherKinds"),
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  wartremoverWarnings in (Compile, compile) ++=
    wartremover.Wart.Any
      :: wartremover.Wart.Var
      :: wartremover.Wart.AnyVal
      :: wartremover.Wart.ArrayEquals
      :: wartremover.Wart.AsInstanceOf
      :: wartremover.Wart.DefaultArguments
      :: wartremover.Wart.EitherProjectionPartial
      :: wartremover.Wart.Enumeration
      :: wartremover.Wart.Equals
      :: wartremover.Wart.ExplicitImplicitTypes
      :: wartremover.Wart.FinalCaseClass
      :: wartremover.Wart.FinalVal
      :: wartremover.Wart.ImplicitConversion
      :: wartremover.Wart.IsInstanceOf
      :: wartremover.Wart.JavaConversions
      :: wartremover.Wart.JavaSerializable
      :: wartremover.Wart.LeakingSealed
      :: wartremover.Wart.MutableDataStructures
      :: wartremover.Wart.NonUnitStatements
      :: wartremover.Wart.Null
      :: wartremover.Wart.Option2Iterable
      :: wartremover.Wart.OptionPartial
      :: wartremover.Wart.Overloading
      :: wartremover.Wart.Product
//  :: wartremover.Wart.Recursion
      :: wartremover.Wart.Return
      :: wartremover.Wart.Serializable
      :: wartremover.Wart.StringPlusAny
      :: wartremover.Wart.Throw
      :: wartremover.Wart.ToString
      :: wartremover.Wart.TraversableOps
      :: wartremover.Wart.TryPartial
      :: wartremover.Wart.While
      :: Nil,
  libraryDependencies ++= Seq(
    "com.github.bigwheel" %% "util-backports" % "2.1"
  ),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
)
addCommandAlias("prePR", "; test; scalafmtCheckAll; test:unusedCompileDependenciesTest")
