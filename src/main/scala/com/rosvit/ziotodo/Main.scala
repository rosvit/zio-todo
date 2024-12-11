package com.rosvit.ziotodo

import com.rosvit.ziotodo.config.database
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.metrics.connectors.micrometer
import zio.metrics.connectors.micrometer.MicrometerConfig
import zio.metrics.jvm.DefaultJvmMetrics

object Main extends ZIOAppDefault {

  override val bootstrap: ULayer[Unit] =
    Runtime.removeDefaultLoggers ++
      SLF4J.slf4j(LogFormat.colored) ++
      Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath().kebabCase)

  def run: Task[Unit] =
    (for {
      _ <- database.migrateFlyway
      _ <- CleanupTask.start().fork
      _ <- Server.serve(HttpApi())
    } yield ())
      .provide(
        Server.default,
        CleanupTask.live,
        DoobieTodoRepository.live,
        database.transactor,
        database.dataSource,
        micrometer.micrometerLayer,
        ZLayer.succeed(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)),
        ZLayer.succeed(MicrometerConfig.default),
        Runtime.enableRuntimeMetrics,
        DefaultJvmMetrics.live.unit
      )
}
