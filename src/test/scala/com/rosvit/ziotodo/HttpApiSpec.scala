package com.rosvit.ziotodo

import org.scalamock.stubs.ZIOStubs
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*
import zio.test.Assertion.*

object HttpApiSpec extends ZIOSpecDefault, ZIOStubs {
  import TestUtils.*

  inline val Prefix = "todo"

  private val findAll = test("list all TODO items") {
    val expected = List(newTodo("t1"), newTodo("t2"), newTodo("t3"))
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.findAll().returnsZIOWith(ZIO.succeed(expected))
        resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix))
        payload <- resp.body.to[List[Todo]]
      } yield assert(payload)(hasSameElements(expected)) && verifyOnce(repo.findAll())
    }
  }

  private val findById = test("get TODO by id") {
    val todo = newTodo("test")
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.findById.returnsZIO {
          case todo.id => ZIO.some(todo)
          case _       => ZIO.none
        }
        resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix / todo.id.toString))
        payload <- resp.body.to[Option[Todo]]
      } yield assertTrue(payload.is(_.some) == todo) && verifyOnce(repo.findById)
    }
  }

  private val findByIdNotFound = test("get TODO by id - not found") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.findById.returnsZIOWith(ZIO.none)
        resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix / id.toString))
      } yield assert(resp.status)(equalTo(Status.NotFound)) && verifyOnceWithArgs(repo.findById, id)
    }
  }

  private val create = test("create new TODO") {
    val expectedTodo = newTodo("create")
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.create.returnsZIOWith(ZIO.succeed(expectedTodo))
        resp <- doRequest(port => Request.post(URL.root.port(port) / Prefix, Body.from(CreateTodo("create"))))
        payload <- resp.body.to[Todo]
      } yield assert(payload)(equalTo(expectedTodo))
        && assert(resp.status)(equalTo(Status.Created))
        && verifyOnceWithArgs(repo.create, expectedTodo.description)
    }
  }

  private val update = test("update completed flag") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.complete.returnsZIOWith(ZIO.succeed(true))
        resp <- doRequest(port => Request.patch(URL.root.port(port) / Prefix / id.toString / "true", Body.empty))
      } yield assert(resp.status)(equalTo(Status.Ok)) && verifyOnceWithArgs(repo.complete, (id, true))
    }
  }

  private val delete = test("delete TODO by id") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    withRepository(repo) {
      for {
        _ <- repo.delete.returnsZIOWith(ZIO.unit)
        resp <- doRequest(port => Request.delete(URL.root.port(port) / Prefix / id.toString))
      } yield assert(resp.status)(equalTo(Status.Ok)) && verifyOnceWithArgs(repo.delete, id)
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("HttpApiSpec")(
    findAll,
    findById,
    findByIdNotFound,
    create,
    update,
    delete
  ).provide(
    TestServer.layer,
    ZLayer.succeed(Server.Config.default.onAnyOpenPort),
    Client.default,
    NettyDriver.customized,
    ZLayer.succeed(NettyConfig.defaultWithFastShutdown)
  )

  private def withRepository(repo: TodoRepository): ULayer[TodoRepository] = ZLayer.succeed(repo)

  private def doRequest(requestFn: Int => Request) = for {
    client <- ZIO.service[Client]
    port <- ZIO.serviceWithZIO[Server](_.port)
    _ <- TestServer.addRoutes(HttpApi.apiRoutes)
    resp <- client.batched(requestFn(port))
  } yield resp
}
