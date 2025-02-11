import Dependencies.*
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "zio-todo",
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true, scala3Sources = true) -> (Compile / sourceManaged).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
    ),
    Compile / scalacOptions += "-Wconf:src=target/scala[^/]*/src_managed/.*:silent", // silence warnings in generated code
    Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
    scalacOptions ++= Seq("-no-indent", "-rewrite"),
    libraryDependencies ++= zioDeps ++ doobieDeps ++ grpcDeps ++ Seq(
      micrometerRegistryPrometheus,
      flywayPostgresql,
      logbackClassic
    ) ++ testDeps
  )
