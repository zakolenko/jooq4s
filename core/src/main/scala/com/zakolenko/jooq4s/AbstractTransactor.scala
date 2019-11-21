package com.zakolenko.jooq4s

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import org.jooq.scalaextensions.Conversions._
import org.jooq.{Cursor, DSLContext, Record, Query => JQuery, ResultQuery => JResultQuery}

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

class AbstractTransactor[F[_]: Sync: ContextShift](
  dsl: DSLContext,
  blocker: Blocker
) extends Transactor[F] {

  override def one[R <: Record](rq: JResultQuery[R]): F[R] = {
    withDslF(_.fetchOne(rq))
  }

  override def option[R <: Record](rq: JResultQuery[R]): F[Option[R]] = {
    withDslF(_.fetchOneOption(rq))
  }

  override def collect[R <: Record, CC[_]](rq: JResultQuery[R])(implicit cbf: CanBuildFrom[Nothing, R, CC[R]]): F[CC[R]] = {
    val acquire: F[Cursor[R]] = blocker.delay(dsl.fetchLazy(rq))
    val use: Cursor[R] => F[CC[R]] = { cursor =>
      blocker.delay {
        val acc: mutable.Builder[R, CC[R]] = cbf.apply()
        val iter: java.util.Iterator[R] = cursor.iterator
        while (iter.hasNext) acc += iter.next
        acc.result
      }
    }
    val release: Cursor[R] => F[Unit] = c => blocker.delay(c.close())

    Sync[F].bracket(acquire)(use)(release)
  }

  override def stream[R <: Record](rq: JResultQuery[R]): fs2.Stream[F, R] = {
    fs2.Stream
      .resource(Resource.fromAutoCloseable(blocker.delay(dsl.fetchLazy(rq))))
      .flatMap(cursor => fs2.Stream.fromBlockingIterator(blocker, cursor.iterator.asScala))
  }

  override def execute(query: JQuery): F[Int] = {
    withDslF(_.execute(query))
  }

  protected def withDslF[T](f: DSLContext => T): F[T] = {
    blocker.delay(f(dsl))
  }
}
