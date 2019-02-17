name := "web"

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=target/.*",
  s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
)
