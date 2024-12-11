package com.rosvit.ziotodo

import cats.implicits.catsSyntaxApplicativeId
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*
import zio.interop.catz.*

import java.time.Instant
import java.util.UUID

trait TodoRepository {
  def findById(id: TodoId): Task[Option[Todo]]
  def findAll(): Task[List[Todo]]
  def create(description: String): Task[Todo]
  def complete(id: TodoId, completed: Boolean): Task[Boolean]
  def deleteCompleted(completedBefore: Instant, lockId: Int): Task[Option[Int]]
}

object TodoRepository {
  def findById(id: TodoId): RIO[TodoRepository, Option[Todo]] =
    ZIO.serviceWithZIO[TodoRepository](_.findById(id))

  def findAll(): RIO[TodoRepository, List[Todo]] =
    ZIO.serviceWithZIO[TodoRepository](_.findAll())

  def create(description: String): RIO[TodoRepository, Todo] =
    ZIO.serviceWithZIO[TodoRepository](_.create(description))

  def complete(id: TodoId, completed: Boolean): RIO[TodoRepository, Boolean] =
    ZIO.serviceWithZIO[TodoRepository](_.complete(id, completed))

  def deleteCompleted(completedBefore: Instant, lockId: Int): RIO[TodoRepository, Option[Int]] =
    ZIO.serviceWithZIO[TodoRepository](_.deleteCompleted(completedBefore, lockId))
}

class DoobieTodoRepository(xa: Transactor[Task]) extends TodoRepository {

  given Meta[TodoId] = Meta[UUID].timap(TodoId(_))(_.value())

  override def findById(id: TodoId): Task[Option[Todo]] =
    sql"SELECT id, description, completed, completed_at FROM todo WHERE id = $id"
      .query[Todo]
      .option
      .transact(xa)

  override def findAll(): Task[List[Todo]] =
    sql"SELECT id, description, completed, completed_at FROM todo"
      .query[Todo]
      .to[List]
      .transact(xa)

  override def create(description: String): Task[Todo] =
    sql"INSERT INTO todo(description) VALUES ($description) RETURNING id"
      .query[TodoId]
      .unique
      .transact(xa)
      .map(id => Todo(id, description, false, None))

  override def complete(id: TodoId, completed: Boolean): Task[Boolean] = {
    val frCompletedAt = if (completed) fr"now()" else fr"NULL"
    sql"UPDATE todo SET completed = $completed, completed_at = $frCompletedAt WHERE id = $id".update.run
      .transact(xa)
      .map(_ > 0)
  }

  override def deleteCompleted(completedBefore: Instant, lockId: Int): Task[Option[Int]] = (for {
    lock <- sql"SELECT pg_try_advisory_xact_lock($lockId);".query[Boolean].unique
    res <-
      if (lock) sql"DELETE FROM todo WHERE completed_at < $completedBefore".update.run.map(Some(_))
      else None.pure[ConnectionIO]
  } yield res).transact(xa)
}

object DoobieTodoRepository {
  lazy val live: RLayer[Transactor[Task], TodoRepository] = ZLayer.fromFunction(DoobieTodoRepository(_))
}
