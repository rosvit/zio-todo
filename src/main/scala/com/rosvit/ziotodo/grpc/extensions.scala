package com.rosvit.ziotodo.grpc

import com.rosvit.ziotodo.Todo
import com.rosvit.ziotodo.grpc_api.Todo as TodoProto

object extensions {

  extension (todo: Todo) {
    def asProto: TodoProto = TodoProto(todo.id, todo.description, todo.completed, todo.completedAt)
  }

  extension (tp: TodoProto) {
    def asDomain: Todo = Todo(tp.id, tp.description, tp.completed, tp.completedAt)
  }
}
