package com.zakolenko.jooq4s

import org.jooq.{Record, ResultQuery => JResultQuery}

import scala.collection.generic.CanBuildFrom

object ResultQuery {

  final class Ops[R <: Record](private val rq: JResultQuery[R]) extends AnyVal {
    def one[F[_]](tr: Transactor[F]): F[R] = tr.one(rq)
    def option[F[_]](tr: Transactor[F]): F[Option[R]] = tr.option(rq)
    def collectTo[CC[_]]: CollectTo[CC, R] = new CollectTo(rq)
    def stream[F[_]](batchSize: Int = 512)(tr: Transactor[F]): fs2.Stream[F, R] = tr.stream(rq, batchSize)
    def execute[F[_]](tr: Transactor[F]): F[Int] = tr.execute(rq)
  }

  final class CollectTo[CC[_], R <: Record](private val rq: JResultQuery[R]) extends AnyVal {
    def apply[F[_]](tr: Transactor[F])(implicit cbf: CanBuildFrom[Nothing, R, CC[R]]): F[CC[R]] = tr.collect(rq)
  }

  trait ToResultQueryOps {
    implicit def toResultQueryOps[R <: Record](rq: JResultQuery[R]): ResultQuery.Ops[R] = new ResultQuery.Ops(rq)
  }
}
