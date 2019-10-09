package com.zakolenko.jooq4s.hikari

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import com.zakolenko.jooq4s.Transactor
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.SQLDialect

import scala.concurrent.ExecutionContext

object HikariTransactor {

  def apply[F[_]: Sync: ContextShift](
    hikariDataSource: HikariDataSource,
    dialect: SQLDialect,
    blocker: Blocker
  ): Transactor[F] = {
    Transactor.fromDataSource(hikariDataSource, dialect, blocker)
  }

  def apply[F[_]: Sync: ContextShift](
    hikariConfig: HikariConfig,
    dialect: SQLDialect,
    blocker: Blocker
  ): Resource[F, Transactor[F]] = {
    val acquire: F[HikariDataSource] = Sync[F].delay(new HikariDataSource(hikariConfig))
    val release: HikariDataSource => F[Unit] = hds => Sync[F].delay(hds.close())

    Resource
      .make(acquire)(release)
      .map(HikariTransactor.apply(_, dialect, blocker))
  }

  def apply[F[_]: Sync: ContextShift](
    driverClassName: String,
    dialect: SQLDialect,
    jdbcUrl: String,
    username: String,
    password: String,
  ): Resource[F, Transactor[F]] = {
    for {
      hikariConfig <- {
        Resource.liftF(Sync[F].delay {
          val hikariConfig = new HikariConfig()
          hikariConfig.setDriverClassName(driverClassName)
          hikariConfig.setJdbcUrl(jdbcUrl)
          hikariConfig.setUsername(username)
          hikariConfig.setPassword(password)
          hikariConfig.validate()
          hikariConfig
        })
      }

      executionContext <- {
        val acquire: F[ExecutorService] = Sync[F].delay(Executors.newFixedThreadPool(hikariConfig.getMaximumPoolSize))
        val release: ExecutorService => F[Unit] = es => Sync[F].delay(es.shutdown())
        Resource.make(acquire)(release).map(ExecutionContext.fromExecutor)
      }

      transactor <- HikariTransactor(hikariConfig, dialect, Blocker.liftExecutionContext(executionContext))
    } yield {
      transactor
    }
  }
}
