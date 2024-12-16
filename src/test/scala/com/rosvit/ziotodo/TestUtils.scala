package com.rosvit.ziotodo

import java.util.UUID

object TestUtils {

  def newTodoId(): TodoId = TodoId(UUID.randomUUID())

  def newTodo(description: String): Todo = Todo(newTodoId(), description, false, None)
}
