package com.zakolenko.jooq4s

import cats.effect.concurrent.Semaphore
import org.jooq.{Query, Record, ResultQuery}

class LimitedTransactor[F[_]](
  underlying: Transactor[F],
  semaphore: Semaphore[F]
) extends Transactor[F] {

  override def one[R <: Record](rq: ResultQuery[R]): F[R] = {
    semaphore.withPermit(underlying.one(rq))
  }

  override def option[R <: Record](rq: ResultQuery[R]): F[Option[R]] = {
    semaphore.withPermit(underlying.option(rq))
  }

  override def stream[R <: Record](rq: ResultQuery[R]): fs2.Stream[F, R] = {
    fs2.Stream
      .bracket(semaphore.acquire)(_ => semaphore.release)
      .flatMap(_ => underlying.stream(rq))
  }

  override def execute(query: Query): F[Int] = {
    semaphore.withPermit(underlying.execute(query))
  }
}
