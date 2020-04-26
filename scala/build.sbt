scalaVersion := "2.13.1"

val http4sVersion = "0.21.2"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "dev.zio" %% "zio" % "1.0.0-RC18-2",
  "dev.zio" %% "zio-test" % "1.0.0-RC18-2",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC12"
)



val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-literal"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client" %% "core" % "2.1.0-RC1",
  "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.1.0-RC1"
)

libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.12.3"


// assembly

assemblyJarName in assembly := "restauthprovider-v0.0.1.jar"
test in assembly := {}

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("MyApp")
