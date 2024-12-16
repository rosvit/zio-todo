package com.rosvit.ziotodo.grpc

import com.google.protobuf.empty.Empty
import com.rosvit.ziotodo.grpc_api.ZioGrpcApi.TodoServiceClient
import com.rosvit.ziotodo.grpc_api.{CreateRequest, IdRequest, UpdateRequest, ZioGrpcApi}
import com.rosvit.ziotodo.{Todo, TodoId, TodoRepository}
import eu.monniot.scala3mock.cats.ScalaMocks.*
import io.grpc.Status
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import scalapb.zio_grpc.{Server, ServerLayer, ZManagedChannel}
import zio.*
import zio.interop.catz.*
import zio.test.*
import zio.test.Assertion.*
import com.rosvit.ziotodo.grpc.extensions.asDomain

import java.util.UUID

object TodoServiceSpec extends ZIOSpecDefault {
  import com.rosvit.ziotodo.TestUtils.*

  private val getAll = test("list all TODO items") {
    withExpectations() {
      val expected = List(newTodo("t1"), newTodo("t2"), newTodo("t3"))
      val repo = mock[TodoRepository]
      when(repo.findAll _)
        .expects()
        .returning(ZIO.succeed(expected))
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.getAll(Empty())
          items = resp.todos.map(_.asDomain)
        } yield assert(items)(hasSameElements(expected))
      }
    }
  }

  private val getById = test("get TODO by id") {
    withExpectations() {
      val todo = newTodo("test")
      val repo = mock[TodoRepository]
      when(repo.findById).expects(todo.id).returning(ZIO.succeed(Some(todo)))
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.getById(IdRequest(todo.id))
        } yield assert(resp.id)(equalTo(todo.id))
      }
    }
  }

  private val getByIdNotFound = test("get TODO by id - not found") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.findById).expects(id).returning(ZIO.succeed(None))
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.getById(IdRequest(id)).mapError(_.getStatus).exit
        } yield assert(resp)(fails(equalTo(Status.NOT_FOUND)))
      }
    }
  }

  private val create = test("create new TODO") {
    withExpectations() {
      val expectedTodo = newTodo("create")
      val repo = mock[TodoRepository]
      when(repo.create).expects(expectedTodo.description).returning(ZIO.succeed(expectedTodo))
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.create(CreateRequest(expectedTodo.description))
        } yield assert(resp.asDomain)(equalTo(expectedTodo))
      }
    }
  }

  private val update = test("update completed flag") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.complete).expects(id, true).returning(ZIO.succeed(true))
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.updateCompleted(UpdateRequest(id, true))
        } yield assert(resp)(equalTo(Empty()))
      }
    }
  }

  private val delete = test("delete TODO by id") {
    withExpectations() {
      val id = newTodoId()
      val repo = mock[TodoRepository]
      when(repo.delete).expects(id).returning(ZIO.unit)
      provideAllLayers(repo) {
        for {
          client <- ZIO.service[TodoServiceClient]
          resp <- client.delete(IdRequest(id))
        } yield assert(resp)(equalTo(Empty()))
      }
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("TodoServiceSpec")(
    getAll,
    getById,
    getByIdNotFound,
    create,
    update,
    delete
  )

  def provideAllLayers(repo: TodoRepository): TaskLayer[TodoServiceClient] = {
    val processName = UUID.randomUUID().toString
    ZLayer.succeed(repo) >>> TodoService.live >>> grpcServer(processName) >>> grpcClient(processName)
  }

  def grpcServer(name: String): RLayer[TodoService, Server] =
    ServerLayer.fromEnvironment[TodoService](InProcessServerBuilder.forName(name).directExecutor())

  def grpcClient(name: String): RLayer[Server, TodoServiceClient] =
    ZLayer.scoped {
      for {
        chan <- ZIO.attempt(InProcessChannelBuilder.forName(name).usePlaintext().directExecutor())
        client <- ZioGrpcApi.TodoServiceClient.scoped(ZManagedChannel(chan))
      } yield client
    }
}
