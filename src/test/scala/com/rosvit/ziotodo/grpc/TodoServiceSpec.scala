package com.rosvit.ziotodo.grpc

import com.google.protobuf.empty.Empty
import com.rosvit.ziotodo.grpc_api.ZioGrpcApi.TodoServiceClient
import com.rosvit.ziotodo.grpc_api.{CreateRequest, IdRequest, UpdateRequest, ZioGrpcApi}
import com.rosvit.ziotodo.{Todo, TodoId, TodoRepository}
import io.grpc.Status
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import scalapb.zio_grpc.{Server, ServerLayer, ZManagedChannel}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.rosvit.ziotodo.grpc.extensions.asDomain
import org.scalamock.stubs.ZIOStubs

import java.util.UUID

object TodoServiceSpec extends ZIOSpecDefault, ZIOStubs {
  import com.rosvit.ziotodo.TestUtils.*

  private val getAll = test("list all TODO items") {
    val expected = List(newTodo("t1"), newTodo("t2"), newTodo("t3"))
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.findAll().returnsZIOWith(ZIO.succeed(expected))
        client <- ZIO.service[TodoServiceClient]
        resp <- client.getAll(Empty())
        items = resp.todos.map(_.asDomain)
      } yield assert(items)(hasSameElements(expected)) && verifyOnce(repo.findAll())
    }
  }

  private val getById = test("get TODO by id") {
    val todo = newTodo("test")
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.findById.returnsZIOWith(ZIO.some(todo))
        client <- ZIO.service[TodoServiceClient]
        resp <- client.getById(IdRequest(todo.id))
      } yield assert(resp.id)(equalTo(todo.id)) && verifyOnceWithArgs(repo.findById, todo.id)
    }
  }

  private val getByIdNotFound = test("get TODO by id - not found") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.findById.returnsZIOWith(ZIO.none)
        client <- ZIO.service[TodoServiceClient]
        resp <- client.getById(IdRequest(id)).mapError(_.getStatus).exit
      } yield assert(resp)(fails(equalTo(Status.NOT_FOUND))) && verifyOnceWithArgs(repo.findById, id)
    }
  }

  private val create = test("create new TODO") {
    val expectedTodo = newTodo("create")
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.create.returnsZIOWith(ZIO.succeed(expectedTodo))
        client <- ZIO.service[TodoServiceClient]
        resp <- client.create(CreateRequest(expectedTodo.description))
      } yield assert(resp.asDomain)(equalTo(expectedTodo)) && verifyOnceWithArgs(repo.create, expectedTodo.description)
    }
  }

  private val update = test("update completed flag") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.complete.returnsZIOWith(ZIO.succeed(true))
        client <- ZIO.service[TodoServiceClient]
        resp <- client.updateCompleted(UpdateRequest(id, true))
      } yield assert(resp)(equalTo(Empty())) && verifyOnceWithArgs(repo.complete, (id, true))
    }
  }

  private val delete = test("delete TODO by id") {
    val id = newTodoId()
    val repo = stub[TodoRepository]
    provideAllLayers(repo) {
      for {
        _ <- repo.delete.returnsZIOWith(ZIO.unit)
        client <- ZIO.service[TodoServiceClient]
        resp <- client.delete(IdRequest(id))
      } yield assert(resp)(equalTo(Empty())) && verifyOnceWithArgs(repo.delete, id)
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
