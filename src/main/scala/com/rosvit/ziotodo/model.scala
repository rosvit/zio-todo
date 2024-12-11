package com.rosvit.ziotodo

import zio.schema.{DeriveSchema, Schema}

import java.time.Instant
import java.util.UUID

opaque type TodoId = UUID
object TodoId {
  given (using ev: Schema[UUID]): Schema[TodoId] = ev
  def apply(uuid: UUID): TodoId = uuid
}

extension (todoId: TodoId) {
  def value(): UUID = todoId
}

final case class Todo(id: TodoId, description: String, completed: Boolean, completedAt: Option[Instant])

object Todo {
  given Schema[Todo] = DeriveSchema.gen
}

final case class CreateTodo(description: String)

object CreateTodo {
  given Schema[CreateTodo] = DeriveSchema.gen
}
