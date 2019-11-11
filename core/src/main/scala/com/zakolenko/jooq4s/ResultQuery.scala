package com.zakolenko.jooq4s

import org.jooq.{Record, ResultQuery}

object ResultQuery {

  final class Ops[R <: Record](private val rq: ResultQuery[R]) extends AnyVal {
    def one[F[_]](tr: Transactor[F]): F[R] = tr.one(rq)
    def option[F[_]](tr: Transactor[F]): F[Option[R]] = tr.option(rq)
    def stream[F[_]](tr: Transactor[F]): fs2.Stream[F, R] = tr.stream(rq)
  }

  trait ToResultQueryOps {
    implicit def toResultQueryOps[R <: Record](rq: ResultQuery[R]): ResultQuery.Ops[R] = new ResultQuery.Ops(rq)
  }
}
