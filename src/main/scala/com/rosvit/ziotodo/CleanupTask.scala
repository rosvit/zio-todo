package com.rosvit.ziotodo

import com.rosvit.ziotodo.config.{AppConfig, CleanupConfig}
import zio.*

class CleanupTask(repository: TodoRepository, config: CleanupConfig) {

  def start(): Task[Unit] = (for {
    before <- Clock.instant.map(_.minusMillis(config.deleteAfter.toMillis))
    _ <- repository
      .deleteCompleted(before, config.lockId)
      .tap(_.fold(ZIO.unit)(count => ZIO.logInfo(s"DB clean-up finished, $count items removed...")))
      .tapErrorCause(c => ZIO.logErrorCause(c))
      .unit
  } yield ())
    .orElse(ZIO.unit)
    .repeat(Schedule.fixed(config.repeatAfter).unit)
}

object CleanupTask {

  def start(): RIO[CleanupTask, Unit] = ZIO.serviceWithZIO[CleanupTask](_.start())

  private given ZLayer.Derive.Default.WithContext[Any, Config.Error, CleanupConfig] =
    ZLayer.Derive.Default.deriveDefaultConfig[AppConfig].map(_.cleanup)

  lazy val live: RLayer[TodoRepository, CleanupTask] = ZLayer.derive[CleanupTask]
}
