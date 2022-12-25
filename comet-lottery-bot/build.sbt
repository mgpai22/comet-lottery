scalaVersion := "2.12.16"

name := "comet-lottery"
organization := "com.comet"
version := "1.0.0"


ThisBuild / version      := "1.0.0"

// Note, it's not required for you to define these three settings. These are
// mostly only necessary if you intend to publish your library's binaries on a
// place like Sonatype.


// Want to use a published library in your project?
// You can define other libraries as dependencies in your build like this:

lazy val NexusReleases = "Sonatype Releases".at(
  "https://s01.oss.sonatype.org/content/repositories/releases"
)

lazy val NexusSnapshots = "Sonatype Snapshots".at(
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
)

resolvers ++= Seq(
  NexusReleases,
  NexusSnapshots
) ++ Resolver.sonatypeOssRepos("public") ++ Resolver.sonatypeOssRepos("snapshots")

val AkkaVersion = "2.7.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-protobuf-v3" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion % Test,
)

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "5.0.0",
  "io.github.ergo-lend" % "edge_2.12" % "0.1-SNAPSHOT",
  "com.google.code.gson" % "gson" % "2.10"
)

libraryDependencies += "com.lihaoyi" %% "requests" % "0.6.9"


libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.7.0"
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Bintray" at "https://jcenter.bintray.com/"
)
assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case "logback.xml" => MergeStrategy.first
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList("org", "bouncycastle", xs @ _*) => MergeStrategy.first
  case PathList("org", "iq80", "leveldb", xs @ _*) => MergeStrategy.first
  case PathList("org", "bouncycastle", xs @ _*) => MergeStrategy.first
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("javax", "annotation", xs @ _*) => MergeStrategy.last
  case "lotteryConf.json" => MergeStrategy.discard
  case "serviceOwner.json" => MergeStrategy.discard
  case other => (assemblyMergeStrategy in assembly).value(other)
}

assemblyJarName in assembly := s"comet-lottery-${version.value}.jar"
assemblyOutputPath in assembly := file(s"./comet-lottery-${version.value}.jar/")
mainClass in assembly := Some("app.Main")

