package com.zakolenko.jooq4s

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import org.jooq.scalaextensions.Conversions._
import org.jooq.{DSLContext, Query, Record, ResultQuery}

import scala.collection.JavaConverters._

class AbstractTransactor[F[_]: Sync: ContextShift](
  dsl: DSLContext,
  blocker: Blocker
) extends Transactor[F] {

  override def one[R <: Record](rq: ResultQuery[R]): F[R] = {
    withDslF(_.fetchOne(rq))
  }

  override def option[R <: Record](rq: ResultQuery[R]): F[Option[R]] = {
    withDslF(_.fetchOneOption(rq))
  }

  override def stream[R <: Record](rq: ResultQuery[R]): fs2.Stream[F, R] = {
    fs2.Stream
      .resource(Resource.fromAutoCloseable(blocker.delay(dsl.fetchLazy(rq))))
      .flatMap(cursor => fs2.Stream.fromBlockingIterator(blocker, cursor.iterator.asScala))
  }

  override def execute(query: Query): F[Int] = {
    withDslF(_.execute(query))
  }

  protected def withDslF[T](f: DSLContext => T): F[T] = {
    blocker.delay(f(dsl))
  }
}
