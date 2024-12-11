package com.rosvit.ziotodo.config

import zio.Config
import zio.config.magnolia.deriveConfig
import zio.Duration

final case class AppConfig(db: DbConfig, cleanup: CleanupConfig)

object AppConfig {
  given Config[AppConfig] = deriveConfig[AppConfig]
}

final case class DbConfig(
    url: String,
    user: String,
    password: String,
    driver: String,
    dbName: String,
    maxPoolSize: Option[Int]
)

object DbConfig {
  inline val DefaultMaxPoolSize = 10
}

final case class CleanupConfig(deleteAfter: Duration, repeatAfter: Duration, lockId: Int)
