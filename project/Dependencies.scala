import sbt.*

object Dependencies {

  val zioVersion = "2.1.13"
  val zioHttpVersion = "3.0.1"
  val zioJsonVersion = "0.7.3"
  val zioConfigVersion = "4.0.2"
  val zioLoggingVersion = "2.4.0"
  val zioMetricsVersion = "2.3.1"
  val zioInteropCatsVersion = "23.1.0.3"
  val doobieVersion = "1.0.0-RC6"
  val micrometerVersion = "1.14.2"
  val flywayVersion = "11.0.1"
  val logbackVersion = "1.5.12"
  val zioTestcontainersVersion = "0.6.0"
  val testcontainersPgVersion = "0.41.4"

  lazy val zio = "dev.zio" %% "zio" % zioVersion
  lazy val zioHttp = "dev.zio" %% "zio-http" % zioHttpVersion
  lazy val zioJson = "dev.zio" %% "zio-json" % zioJsonVersion
  lazy val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  lazy val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion
  lazy val zioInteropCats = "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion
  lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
  lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  lazy val zioMetricsMicrometer = "dev.zio" %% "zio-metrics-connectors-micrometer" % zioMetricsVersion

  lazy val zioTest = "dev.zio" %% "zio-test" % zioVersion % Test
  lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
  lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
  lazy val zioTestcontainers = "com.github.sideeffffect" %% "zio-testcontainers" % zioTestcontainersVersion % Test
  lazy val testcontainersPg = "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersPgVersion % Test

  lazy val zioDeps: Seq[ModuleID] = Seq(
    zio,
    zioHttp,
    zioJson,
    zioLogging,
    zioLoggingSlf4j,
    zioInteropCats,
    zioConfigTypesafe,
    zioConfigMagnolia,
    zioMetricsMicrometer,
    zioTest,
    zioTestSbt,
    zioTestMagnolia,
    zioTestcontainers,
    testcontainersPg
  )

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  lazy val doobieDeps: Seq[ModuleID] = Seq(doobieCore, doobiePostgres, doobieHikari)

  lazy val micrometerRegistryPrometheus = "io.micrometer" % "micrometer-registry-prometheus" % micrometerVersion
  lazy val flywayPostgresql = "org.flywaydb" % "flyway-database-postgresql" % flywayVersion
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
}
