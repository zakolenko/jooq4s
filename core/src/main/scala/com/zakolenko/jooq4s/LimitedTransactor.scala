package com.zakolenko.jooq4s

import cats.effect.concurrent.Semaphore
import org.jooq.{Record, Query => JQuery, ResultQuery => JResultQuery}

class LimitedTransactor[F[_]](
  underlying: Transactor[F],
  semaphore: Semaphore[F]
) extends Transactor[F] {

  override def one[R <: Record](rq: JResultQuery[R]): F[R] = {
    semaphore.withPermit(underlying.one(rq))
  }

  override def option[R <: Record](rq: JResultQuery[R]): F[Option[R]] = {
    semaphore.withPermit(underlying.option(rq))
  }

  override def stream[R <: Record](rq: JResultQuery[R]): fs2.Stream[F, R] = {
    fs2.Stream
      .bracket(semaphore.acquire)(_ => semaphore.release)
      .flatMap(_ => underlying.stream(rq))
  }

  override def execute(query: JQuery): F[Int] = {
    semaphore.withPermit(underlying.execute(query))
  }
}
