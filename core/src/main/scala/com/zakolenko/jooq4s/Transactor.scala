package com.zakolenko.jooq4s

import cats.effect.{Blocker, ContextShift, Sync}
import javax.sql.DataSource
import org.jooq.impl.DSL
import org.jooq.{Query, Record, ResultQuery, SQLDialect}

trait Transactor[F[_]] {

  def one[R <: Record](rq: ResultQuery[R]): F[R]

  def option[R <: Record](rq: ResultQuery[R]): F[Option[R]]

  def stream[R <: Record](rq: ResultQuery[R]): fs2.Stream[F, R]

  def execute(query: Query): F[Int]
}

object Transactor {

  def fromDataSource[F[_]: Sync: ContextShift](
    dataSource: DataSource,
    dialect: SQLDialect,
    blocker: Blocker
  ): Transactor[F] = {
    new AbstractTransactor[F](DSL.using(dataSource, dialect), blocker)
  }
}