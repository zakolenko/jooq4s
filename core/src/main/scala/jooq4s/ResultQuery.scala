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
