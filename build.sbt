ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.shokohara"
ThisBuild / organizationName := "Sho Kohara"

lazy val root = (project in file(".")).aggregate(seed, lol, slack, web)
lazy val slack = (project in file("slack"))
  .settings(commonSettings)
  .settings(
    scalacOptions +=  "-Ypartial-unification",
    libraryDependencies ++= Seq(
      scalaTest,
      "com.github.seratch" % "jslack" % "1.1.7",
      "jp.t2v" %% "holidays" % "5.2",
      "eu.timepit" %% "refined" % refinedVersion,
      "eu.timepit" %% "refined-pureconfig" % refinedVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.10.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.typelevel" %% "kittens" % "1.2.1",
      "io.chrisdavenport" % "cats-time_2.12" % "0.2.0",
      "org.typelevel" %% "cats-effect" % "1.2.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-specs2" % doobieVersion,
      "com.lihaoyi" %% "sourcecode" % "0.1.5"
    ) ++ commonLibraryDependencies
  )
lazy val seed = (project in file("seed")).settings(
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
      "be.venneborg" %% "play26-refined" % "0.3.0",
      "com.softwaremill.macwire" %% "macros" % "2.3.1" % Provided,
      "com.dripower" %% "play-circe" % "2711.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-java8" % circeVersion,
      "io.circe" %% "circe-refined" % circeVersion,
      "com.typesafe.play" %% "play-json" % "2.7.1", // 一時的にsbt-play-swaggerのために必要
      "org.webjars" % "swagger-ui" % "2.2.0",
    ) ++ silencers ++ commonLibraryDependencies,
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
    mappings in Docker += (swaggerTarget.value / swaggerFileName.value) -> s"public/${swaggerFileName.value}",
    swaggerDomainNameSpaces := Seq("models"),
    dockerBaseImage := "openjdk:8u181-jdk-stretch",
    daemonUser in Docker := "root",
    dockerEntrypoint := Seq("/bin/sh", "-c"),
    dockerCmd := Seq("/opt/docker/bin/web"),
  )
  .enablePlugins(BuildInfoPlugin, PlayScala, SwaggerPlugin)
  .dependsOn(slack)

lazy val circeVersion = "0.11.1"
lazy val refinedVersion = "0.9.4"
lazy val doobieVersion = "0.6.0"

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

lazy val silencers = {
  val version = "1.3.1"
  "com.github.ghik" %% "silencer-lib" % version % Provided ::
    compilerPlugin("com.github.ghik" %% "silencer-plugin" % version) :: Nil
}

lazy val commonLibraryDependencies = Seq(
  "com.github.bigwheel" %% "util-backports" % "1.1"
)
lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:higherKinds"),
  scalacOptions in(Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  wartremoverWarnings in(Compile, compile) ++=
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
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
)
addCommandAlias("fmt", "; compile:scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("prePR", "; fmt; test")
