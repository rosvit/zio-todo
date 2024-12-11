package com.rosvit.ziotodo

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import zio.*
import zio.http.*
import zio.http.codec.PathCodec
import zio.schema.Schema
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import java.util.UUID

object HttpApi {

  private inline val PrefixTodo = "todo"
  private inline val PrefixMetrics = "metrics"

  private val todoId: PathCodec[TodoId] = PathCodec.uuid("todoId").transform(TodoId(_))(_.value())

  def apply(): Routes[TodoRepository & PrometheusMeterRegistry, Response] = apiRoutes ++ metricsRoute

  private val apiRoutes: Routes[TodoRepository, Response] =
    Routes(
      Method.GET / PrefixTodo -> handler {
        TodoRepository
          .findAll()
          .map(todos => Response(body = Body.from(todos)))
      },
      Method.GET / PrefixTodo / uuid("todoId") -> handler { (todoId: UUID, _: Request) =>
        TodoRepository
          .findById(TodoId(todoId))
          .map(_.fold(Response.notFound)(todo => Response(body = Body.from(todo))))
      },
      Method.POST / PrefixTodo -> handler { (req: Request) =>
        for {
          ct <- req.body.to[CreateTodo].orElseFail(Response.badRequest)
          res <- TodoRepository
            .create(ct.description)
            .map(created => Response(body = Body.from(created)))
        } yield res
      },
      Method.PATCH / PrefixTodo / todoId / boolean("completed") -> handler {
        (todoId: TodoId, completed: Boolean, _: Request) =>
          TodoRepository
            .complete(todoId, completed)
            .map(res => if (res) Response.ok else Response.notFound)
      }
    ).handleErrorCauseZIO { cause =>
      ZIO.logErrorCause(cause).map(_ => Response.fromCause(cause))
    } @@ Middleware.metrics()

  private val metricsRoute: Routes[PrometheusMeterRegistry, Response] =
    Routes(
      Method.GET / PrefixMetrics ->
        handler(ZIO.serviceWith[PrometheusMeterRegistry](m => Response.text(m.scrape())))
    )
}
