package com.rosvit.ziotodo

import eu.monniot.scala3mock.cats.ScalaMocks.*
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.interop.catz.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID

object HttpApiSpec extends ZIOSpecDefault {

  inline val Prefix = "todo"

  private val findAll = test("list all TODO items") {
    withExpectations() {
      val expected = List(newTodo("t1"), newTodo("t2"), newTodo("t3"))
      val repo = mock[TodoRepository]
      when(repo.findAll _)
        .expects()
        .returning(ZIO.succeed(expected))
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix))
          payload <- resp.body.to[List[Todo]]
        } yield assert(payload)(hasSameElements(expected))
      }
    }
  }

  private val findById = test("get TODO by id") {
    withExpectations() {
      val todo = newTodo("test")
      val repo = mock[TodoRepository]
      when(repo.findById).expects(todo.id).returning(ZIO.succeed(Some(todo)))
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix / todo.id.toString))
          payload <- resp.body.to[Option[Todo]]
        } yield assertTrue(payload.is(_.some) == todo)
      }
    }
  }

  private val findByIdNotFound = test("get TODO by id - not found") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.findById).expects(id).returning(ZIO.succeed(None))
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.get(URL.root.port(port) / Prefix / id.toString))
        } yield assert(resp.status)(equalTo(Status.NotFound))
      }
    }
  }

  private val create = test("create new TODO") {
    withExpectations() {
      val expectedTodo = newTodo("create")
      val repo = mock[TodoRepository]
      when(repo.create).expects(expectedTodo.description).returning(ZIO.succeed(expectedTodo))
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.post(URL.root.port(port) / Prefix, Body.from(CreateTodo("create"))))
          payload <- resp.body.to[Todo]
        } yield assert(payload)(equalTo(expectedTodo)) && assert(resp.status)(equalTo(Status.Created))
      }
    }
  }

  private val update = test("update completed flag") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.complete).expects(id, true).returning(ZIO.succeed(true))
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.patch(URL.root.port(port) / Prefix / id.toString / "true", Body.empty))
        } yield assert(resp.status)(equalTo(Status.Ok))
      }
    }
  }

  private val delete = test("delete TODO by id") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.delete).expects(id).returning(ZIO.unit)
      val mockLayer = ZLayer.succeed(repo)

      mockLayer {
        for {
          resp <- doRequest(port => Request.delete(URL.root.port(port) / Prefix / id.toString))
        } yield assert(resp.status)(equalTo(Status.Ok))
      }
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("HttpApi")(
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

  private def doRequest(requestFn: Int => Request) = for {
    client <- ZIO.service[Client]
    port <- ZIO.serviceWithZIO[Server](_.port)
    _ <- TestServer.addRoutes(HttpApi.apiRoutes)
    resp <- client.batched(requestFn(port))
  } yield resp

  private def newTodoId(): TodoId = TodoId(UUID.randomUUID())

  private def newTodo(description: String): Todo = Todo(newTodoId(), description, false, None)
}
