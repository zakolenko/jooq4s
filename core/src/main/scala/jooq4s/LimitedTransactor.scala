/*
 * Copyright (c) 2020 the jooq4s contributors.
 * See the project homepage at: https://zakolenko.github.io/jooq4s/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jooq4s

import cats.effect.concurrent.Semaphore
import org.jooq.{Record, Query => JQuery, ResultQuery => JResultQuery}

import scala.collection.generic.CanBuildFrom

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

  override def collect[R <: Record, CC[_]](rq: JResultQuery[R])
                                          (implicit cbf: CanBuildFrom[Nothing, R, CC[R]]): F[CC[R]] = {
    semaphore.withPermit(underlying.collect(rq))
  }

  override def stream[R <: Record](rq: JResultQuery[R], batch: Int): fs2.Stream[F, R] = {
    fs2.Stream
      .bracket(semaphore.acquire)(_ => semaphore.release)
      .flatMap(_ => underlying.stream(rq, batch))
  }

  override def execute(query: JQuery): F[Int] = {
    semaphore.withPermit(underlying.execute(query))
  }
}
