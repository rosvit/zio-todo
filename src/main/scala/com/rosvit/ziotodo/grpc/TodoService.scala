package com.rosvit.ziotodo.grpc

import com.google.protobuf.empty.Empty
import com.rosvit.ziotodo.config.GrpcConfig
import com.rosvit.ziotodo.grpc_api.{Todo as TodoProto, *}
import com.rosvit.ziotodo.{Todo, TodoRepository}
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ServerBuilder, Status, StatusException}
import scalapb.zio_grpc.{Server, ServerLayer}
import zio.*

class TodoService(repository: TodoRepository) extends ZioGrpcApi.TodoService {
  import extensions.asProto

  override def getById(request: IdRequest): IO[StatusException, TodoProto] =
    repository
      .findById(request.id)
      .mapError(_ => Status.INTERNAL.asException())
      .flatMap {
        case Some(res) => ZIO.succeed(res.asProto)
        case None      => ZIO.fail(Status.NOT_FOUND.asException())
      }

  override def getAll(request: Empty): IO[StatusException, TodoList] =
    repository
      .findAll()
      .map(res => TodoList(res.map(_.asProto)))
      .orElseFail(Status.INTERNAL.asException())

  override def create(request: CreateRequest): IO[StatusException, TodoProto] =
    repository.create(request.description).map(_.asProto).orElseFail(Status.INTERNAL.asException())

  override def updateCompleted(request: UpdateRequest): IO[StatusException, Empty] =
    repository
      .complete(request.id, request.completed)
      .mapError(_ => Status.INTERNAL.asException())
      .flatMap(updated => if (updated) ZIO.succeed(Empty()) else ZIO.fail(Status.NOT_FOUND.asException()))

  override def delete(request: IdRequest): IO[StatusException, Empty] =
    repository.delete(request.id).map(_ => Empty()).orElseFail(Status.INTERNAL.asException())
}

object TodoService {

  lazy val live: RLayer[TodoRepository, TodoService] = ZLayer.derive[TodoService]
}

object GrpcServer {

  lazy val live: RLayer[TodoRepository, Server] =
    ZLayer(ZIO.config[GrpcConfig]).flatMap { env =>
      val port = env.get[GrpcConfig].port
      ServerLayer.fromServiceLayer(ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance()))(
        TodoService.live
      )
    }

  def logServerPort: RIO[Server, Unit] =
    ZIO.service[Server].flatMap(_.port).flatMap(p => ZIO.logInfo(s"GRPC server started on port $p..."))
}
