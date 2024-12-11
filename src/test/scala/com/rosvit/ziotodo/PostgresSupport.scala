package com.rosvit.ziotodo

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.Transactor
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.testcontainers.*
import zio.interop.catz.*
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT

import java.sql.DriverManager
import java.util.UUID

trait PostgresSupport {

  lazy val pgLayer: ULayer[PostgreSQLContainer] = PostgreSQLContainer(DockerImageName.parse("postgres:16")).toLayer

  def createDatabase(pg: PostgreSQLContainer): Task[String] = for {
    dbName <- ZIO.succeed(UUID.randomUUID().toString)
    jdbcUrl = s"jdbc:postgresql://${pg.container.getHost}:${pg.container.getMappedPort(POSTGRESQL_PORT)}"
    _ <- createDatabase(s"$jdbcUrl/", pg.username, pg.password, dbName)
  } yield s"$jdbcUrl/$dbName"

  private def createDatabase(jdbcUrl: String, username: String, password: String, dbName: String): Task[Unit] = ZIO
    .scoped {
      for {
        sqlConn <- ZIO.fromAutoCloseable(
          ZIO.attemptBlocking(DriverManager.getConnection(s"$jdbcUrl?user=$username&password=$password"))
        )
        _ <- ZIO.attemptBlocking(sqlConn.prepareStatement(s"""CREATE DATABASE "$dbName";""").execute()).unit
      } yield ()
    }

  def migrateSchema(jdbcUrl: String, username: String, password: String): Task[Unit] = ZIO
    .attemptBlocking(
      Flyway
        .configure()
        .dataSource(jdbcUrl, username, password)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    )
    .unit

  def mkTransactor(jdbcUrl: String, username: String, password: String): Task[Transactor[Task]] = ZIO.attemptBlocking {
    Transactor.fromDriverManager[Task](
      driver = "org.postgresql.Driver",
      url = jdbcUrl,
      user = username,
      password = password,
      logHandler = None
    )
  }
}
