package com.rosvit.ziotodo

import org.scalamock.stubs.StubbedZIOMethod
import zio.test.Assertion.equalTo
import zio.test.{TestResult, assert}

import java.util.UUID

object TestUtils {

  def newTodoId(): TodoId = TodoId(UUID.randomUUID())

  def newTodo(description: String): Todo = Todo(newTodoId(), description, false, None)

  def verifyOnce(m: StubbedZIOMethod[_, _]): TestResult = assert(m.times)(equalTo(1))

  def verifyOnceWithArgs[A](m: StubbedZIOMethod[A, _], expected: A): TestResult =
    assert(m.times)(equalTo(1)) && assert(m.calls)(equalTo(List(expected)))
}
