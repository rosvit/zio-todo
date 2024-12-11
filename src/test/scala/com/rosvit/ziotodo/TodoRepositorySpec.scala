package com.rosvit.ziotodo

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.{after, sequential}

import java.time.Instant

/** [[TodoRepositorySpec]] shows how to test a repository using single shared PostgreSQL testcontainer. Tests are
  * running sequentially and after each test a database cleanup is performed. This is done using ZIO Test aspects. For
  * the parallel alternative please see [[TodoRepositoryParallelSpec]].
  */
object TodoRepositorySpec extends ZIOSpecDefault with PostgresSupport {

  override def spec: Spec[TestEnvironment with Scope, Any] = (suite("TodoRepositorySpec")(
    test("create / findById") {
      for {
        inserted <- TodoRepository.create("test1")
        selected <- TodoRepository.findById(inserted.id)
      } yield assert(selected)(isSome(equalTo(inserted)))
    },
    test("findAll") {
      for {
        _ <- TodoRepository.create("test")
        all <- TodoRepository.findAll()
      } yield assert(all)(hasSize(equalTo(1)))
    },
    test("complete") {
      for {
        todo <- TodoRepository.create("toBeCompleted")
        _ <- TodoRepository.complete(todo.id, true)
        completed <- TodoRepository.findById(todo.id)
      } yield assertTrue(completed.is(_.some).completed) &&
        assertTrue(completed.is(_.some).completedAt.is(_.some.anything))
    },
    test("deleteCompleted") {
      for {
        oldTodo <- TodoRepository.create("old")
        _ <- TodoRepository.complete(oldTodo.id, true)
        _ <- TestClock.setTime(Instant.now())
        _ <- TestClock.adjust(1.hour)
        threshold <- Clock.instant
        delRes <- TodoRepository.deleteCompleted(threshold, 1)
        res <- TodoRepository.findAll()
      } yield assertTrue(delRes.is(_.some) == 1) && assert(res)(isEmpty)
    }
  ) @@ after(clearDb) @@ sequential)
    .provideShared(pgLayer, xaLayer, DoobieTodoRepository.live)

  lazy val xaLayer: RLayer[PostgreSQLContainer, Transactor[Task]] = ZLayer {
    for {
      pg <- ZIO.service[PostgreSQLContainer]
      _ <- migrateSchema(pg.jdbcUrl, pg.username, pg.password)
      xa <- mkTransactor(pg.jdbcUrl, pg.username, pg.password)
    } yield xa
  }

  lazy val clearDb: RIO[Transactor[Task], Unit] = ZIO.serviceWithZIO[Transactor[Task]] { xa =>
    sql"TRUNCATE todo".update.run.transact(xa).unit
  }
}
