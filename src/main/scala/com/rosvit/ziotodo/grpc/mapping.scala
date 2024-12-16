package com.rosvit.ziotodo.grpc

import com.google.protobuf.timestamp.Timestamp
import com.rosvit.ziotodo.TodoId
import scalapb.TypeMapper

import java.time.Instant
import java.util.UUID

object mapping {

  given TypeMapper[String, TodoId] = TypeMapper[String, TodoId](str => TodoId(UUID.fromString(str)))(_.toString)

  given TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant](_.asJavaInstant)(inst => Timestamp.of(inst.getEpochSecond, inst.getNano))
}
