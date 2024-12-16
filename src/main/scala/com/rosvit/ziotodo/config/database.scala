package com.rosvit.ziotodo.config

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import doobie.hikari.{HikariTransactor, Config as DoobieHikariConfig}
import doobie.util.transactor.Transactor
import io.micrometer.core.instrument.MeterRegistry
import org.flywaydb.core.Flyway
import zio.interop.catz.*
import zio.{RIO, RLayer, Task, ZIO, ZLayer}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object database {

  private lazy val mkDatasource: RIO[MeterRegistry, HikariDataSource] = for {
    registry <- ZIO.service[MeterRegistry]
    dbConfig <- ZIO.config[DbConfig]
    hikariCfg <- DoobieHikariConfig.makeHikariConfig[Task](
      DoobieHikariConfig(
        jdbcUrl = dbConfig.url,
        username = Some(dbConfig.user),
        password = Some(dbConfig.password),
        driverClassName = Some(dbConfig.driver),
        maximumPoolSize = dbConfig.maxPoolSize.getOrElse(DbConfig.DefaultMaxPoolSize)
      ),
      metricsTrackerFactory = Some(MicrometerMetricsTrackerFactory(registry))
    )
    ds <- ZIO.attempt(new HikariDataSource(hikariCfg))
  } yield ds

  lazy val dataSource: RLayer[MeterRegistry, HikariDataSource] =
    ZLayer.scoped {
      ZIO.acquireRelease(mkDatasource) { ds =>
        ZIO.attempt(ds.close()).orDie
      }
    }

  lazy val transactor: RLayer[HikariDataSource, Transactor[Task]] = ZLayer {
    for {
      ds <- ZIO.service[HikariDataSource]
      cfg <- ZIO.config[DbConfig]
    } yield HikariTransactor[Task](
      hikariDataSource = ds,
      connectEC = ExecutionContext.fromExecutorService(
        // same size as HikariCP maximumPoolSize, see https://typelevel.org/doobie/docs/14-Managing-Connections.html
        Executors.newFixedThreadPool(cfg.maxPoolSize.getOrElse(DbConfig.DefaultMaxPoolSize))
      )
    )
  }

  lazy val migrateFlyway: RIO[HikariDataSource, Unit] = ZIO
    .serviceWithZIO[HikariDataSource] { ds =>
      ZIO.attemptBlocking(Flyway.configure().dataSource(ds).baselineOnMigrate(true).load().migrate()).unit
    }
}
