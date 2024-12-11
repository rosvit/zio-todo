package com.rosvit.ziotodo

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.rosvit.ziotodo.TodoRepositorySpec.test
import doobie.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant

/** [[TodoRepositoryParallelSpec]] shows how to run tests in parallel while using only single PostgreSQL container. The
  * only shared layer is layer providing testcontainer itself. In order to run tests in parallel we need to create own
  * database for each test. Then we provide own [[TodoRepository]] for each test using [[withRepository]] fixture, which
  * uses own Doobie transactor [[xaLayer]] for each test database.
  */
object TodoRepositoryParallelSpec extends ZIOSpecDefault with PostgresSupport {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("TodoRepositoryParallelSpec")(
    test("create / findById") {
      withRepository {
        for {
          inserted <- TodoRepository.create("test1")
          selected <- TodoRepository.findById(inserted.id)
        } yield assert(selected)(isSome(equalTo(inserted)))
      }
    },
    test("findAll") {
      withRepository {
        for {
          _ <- TodoRepository.create("test")
          all <- TodoRepository.findAll()
        } yield assert(all)(hasSize(equalTo(1)))
      }
    },
    test("complete") {
      withRepository {
        for {
          todo <- TodoRepository.create("toBeCompleted")
          _ <- TodoRepository.complete(todo.id, true)
          completed <- TodoRepository.findById(todo.id)
        } yield assertTrue(completed.is(_.some).completed) &&
          assertTrue(completed.is(_.some).completedAt.is(_.some.anything))
      }
    },
    test("deleteCompleted") {
      withRepository {
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
    }
  ).provideShared(pgLayer)

  lazy val xaLayer: RLayer[PostgreSQLContainer, Transactor[Task]] = ZLayer {
    for {
      pg <- ZIO.service[PostgreSQLContainer]
      jdbcUrl <- createDatabase(pg)
      _ <- migrateSchema(jdbcUrl, pg.username, pg.password)
      xa <- mkTransactor(jdbcUrl, pg.username, pg.password)
    } yield xa
  }

  lazy val withRepository: RLayer[PostgreSQLContainer, TodoRepository] = xaLayer >>> DoobieTodoRepository.live
}
