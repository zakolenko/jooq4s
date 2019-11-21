package com.zakolenko.jooq4s

import cats.effect.{Blocker, ContextShift, Sync}
import javax.sql.DataSource
import org.jooq.impl.DSL
import org.jooq.{Record, SQLDialect, Query => JQuery, ResultQuery => JResultQuery}

import scala.collection.generic.CanBuildFrom

trait Transactor[F[_]] {

  def one[R <: Record](rq: JResultQuery[R]): F[R]

  def option[R <: Record](rq: JResultQuery[R]): F[Option[R]]

  def collect[R <: Record, CC[_]](rq: JResultQuery[R])(implicit cbf: CanBuildFrom[Nothing, R, CC[R]]): F[CC[R]]

  def stream[R <: Record](rq: JResultQuery[R]): fs2.Stream[F, R]

  def execute(query: JQuery): F[Int]
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