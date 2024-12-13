import Dependencies.*
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "zio-todo",
    Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
    scalacOptions ++= Seq("-no-indent", "-rewrite"),
    libraryDependencies ++= zioDeps ++ doobieDeps ++ Seq(
      micrometerRegistryPrometheus,
      flywayPostgresql,
      logbackClassic
    ) ++ testDeps
  )
