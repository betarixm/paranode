ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(
  scalaVersion.value
)
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

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
    "io.reactivex.rxjava3" % "rxjava" % "3.0.4"
  )
)

lazy val root = (project in file("."))
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

lazy val master = (project in file("master"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.master")
  )
  .dependsOn(core)

lazy val worker = (project in file("worker"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.worker")
  )
  .dependsOn(core)

lazy val rpc = (project in file("rpc"))
  .settings(
    commonSettings,
    idePackagePrefix := Some("kr.ac.postech.paranode.rpc"),
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .dependsOn(core)
