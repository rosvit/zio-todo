import sbt.*

object Dependencies {

  val zioVersion = "2.1.17"
  val zioHttpVersion = "3.2.0"
  val zioJsonVersion = "0.7.43"
  val zioConfigVersion = "4.0.4"
  val zioLoggingVersion = "2.5.0"
  val zioMetricsVersion = "2.3.1"
  val zioInteropCatsVersion = "23.1.0.5"
  val doobieVersion = "1.0.0-RC9"
  val micrometerVersion = "1.14.6"
  val flywayVersion = "11.8.0"
  val logbackVersion = "1.5.18"
  val grpcVersion = "1.72.0"
  val googleProtosVersion = "2.9.6-0"

  val zioTestcontainersVersion = "0.6.0"
  val testcontainersPgVersion = "0.43.0"
  val scalaMockVersion = "7.3.2"

  lazy val zio = "dev.zio" %% "zio" % zioVersion
  lazy val zioHttp = "dev.zio" %% "zio-http" % zioHttpVersion
  lazy val zioJson = "dev.zio" %% "zio-json" % zioJsonVersion
  lazy val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  lazy val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion
  lazy val zioInteropCats = "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion
  lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
  lazy val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  lazy val zioMetricsMicrometer = "dev.zio" %% "zio-metrics-connectors-micrometer" % zioMetricsVersion

  lazy val zioDeps: Seq[ModuleID] = Seq(
    zio,
    zioHttp,
    zioJson,
    zioLogging,
    zioLoggingSlf4j,
    zioInteropCats,
    zioConfigTypesafe,
    zioConfigMagnolia,
    zioMetricsMicrometer
  )

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  lazy val doobieDeps: Seq[ModuleID] = Seq(doobieCore, doobiePostgres, doobieHikari)

  lazy val micrometerRegistryPrometheus = "io.micrometer" % "micrometer-registry-prometheus" % micrometerVersion
  lazy val flywayPostgresql = "org.flywaydb" % "flyway-database-postgresql" % flywayVersion
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion

  lazy val grpcNetty = "io.grpc" % "grpc-netty" % grpcVersion
  lazy val scalapbRuntimeGrpc =
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  lazy val googleProtos =
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % googleProtosVersion
  lazy val googleProtosPb =
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % googleProtosVersion % "protobuf"
  lazy val scalaPbProto =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  lazy val grpcInprocess = "io.grpc" % "grpc-inprocess" % grpcVersion % Test

  lazy val grpcDeps: Seq[ModuleID] =
    Seq(grpcNetty, scalapbRuntimeGrpc, googleProtos, googleProtosPb, scalaPbProto, grpcInprocess)

  lazy val zioTest = "dev.zio" %% "zio-test" % zioVersion % Test
  lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
  lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
  lazy val zioHttpTestkit = "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test
  lazy val zioTestcontainers = "com.github.sideeffffect" %% "zio-testcontainers" % zioTestcontainersVersion % Test
  lazy val scalaMockZio = "org.scalamock" %% "scalamock-zio" % scalaMockVersion % Test
  lazy val testcontainersPg = "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersPgVersion % Test

  lazy val testDeps: Seq[ModuleID] = Seq(
    zioTest,
    zioTestSbt,
    zioTestMagnolia,
    zioHttpTestkit,
    zioTestcontainers,
    scalaMockZio,
    testcontainersPg
  )
}
