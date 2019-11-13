package com.zakolenko.jooq4s.hikari

import cats.effect.concurrent.Semaphore
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import cats.implicits._
import com.zakolenko.jooq4s.util.{ExecutionContexts, Resources}
import com.zakolenko.jooq4s.{LimitedTransactor, Transactor}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.SQLDialect

object HikariTransactor {

  private def apply[F[_]: Concurrent: ContextShift](
    hikariDataSource: HikariDataSource,
    dialect: SQLDialect,
    blocker: Blocker,
    limited: Boolean
  ): F[Transactor[F]] = {
    Semaphore
      .apply[F](hikariDataSource.getMaximumPoolSize)
      .map { semaphore =>
        val transactor = Transactor.fromDataSource(hikariDataSource, dialect, blocker)
        if (limited) new LimitedTransactor(transactor, semaphore)
        else transactor
      }
  }

  def apply[F[_]: Concurrent: ContextShift](
    hikariDataSource: HikariDataSource,
    dialect: SQLDialect,
    blocker: Blocker
  ): F[Transactor[F]] = {
    HikariTransactor(hikariDataSource, dialect, blocker, limited = true)
  }

  def apply[F[_]: Concurrent: ContextShift](
    hikariConfig: HikariConfig,
    dialect: SQLDialect,
    blocker: Blocker
  ): Resource[F, Transactor[F]] = {
    Resources
      .make(new HikariDataSource(hikariConfig))(_.close())
      .evalMap(HikariTransactor.apply(_, dialect, blocker))
  }

  def apply[F[_]: Concurrent: ContextShift](
    driverClassName: String,
    dialect: SQLDialect,
    jdbcUrl: String,
    username: String,
    password: String
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

      executionContext <- ExecutionContexts.fixed(hikariConfig.getMaximumPoolSize)
      dataSource <- Resources.make(new HikariDataSource(hikariConfig))(_.close())

      transactor <- {
        Resource.liftF(HikariTransactor.apply(
          dataSource,
          dialect,
          Blocker.liftExecutionContext(executionContext),
          limited = false
        ))
      }
    } yield {
      transactor
    }
  }
}
