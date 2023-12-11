import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(
  scalaVersion.value
)
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / assembly / assemblyMergeStrategy := {
  case "module-info.class"                     => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
  case x => (assembly / assemblyMergeStrategy).value(x)
}

ThisBuild / assemblyPrependShellScript := Some(
  defaultShellScript
)

lazy val commonSettings = Seq(
  name := "cs434-project",
  idePackagePrefix := Some("kr.ac.postech.paranode"),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalacOptions += "-Wunused",
  libraryDependencies ++= Seq(
    "org.scalactic" %% "scalactic" % "3.2.17",
    "org.scalatest" %% "scalatest" % "3.2.17" % "test",
    "org.scalatest" %% "scalatest-flatspec" % "3.2.17" % "test",
    "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    "org.apache.logging.log4j" %% "log4j-api-scala" % "13.0.0",
    "org.apache.logging.log4j" % "log4j-core" % "2.22.0" % Runtime
  )
)

lazy val paranode = (project in file("."))
  .aggregate(core, utils, master, worker, rpc)

lazy val utils = (project in file("utils"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.utils")
  )

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.core")
  )
  .dependsOn(utils)

lazy val rpc = (project in file("rpc"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.rpc"),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .dependsOn(core)

lazy val master = (project in file("master"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.master"),
    assembly / assemblyOutputPath := file("build/master")
  )
  .dependsOn(core)
  .dependsOn(rpc)

lazy val worker = (project in file("worker"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.worker"),
    assembly / assemblyOutputPath := file("build/worker")
  )
  .dependsOn(core)
  .dependsOn(rpc)

lazy val e2e = (project in file("e2e"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.e2e")
  )
  .dependsOn(master)
  .dependsOn(worker)
