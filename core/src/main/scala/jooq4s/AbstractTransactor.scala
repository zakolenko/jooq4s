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

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import cats.implicits._
import org.jooq.scalaextensions.Conversions._
import org.jooq.{Cursor, DSLContext, Record, Query => JQuery, ResultQuery => JResultQuery}

import scala.collection.generic.CanBuildFrom

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
        val acc: collection.mutable.Builder[R, CC[R]] = cbf.apply()
        val iter: java.util.Iterator[R] = cursor.iterator
        while (iter.hasNext) acc += iter.next
        acc.result
      }
    }
    val release: Cursor[R] => F[Unit] = c => blocker.delay(c.close())

    Sync[F].bracket(acquire)(use)(release)
  }

  override def stream[R <: Record](rq: JResultQuery[R], batch: Int = 512): fs2.Stream[F, R] = {
    rq.fetchSize(batch)

    fs2.Stream
      .resource(Resource.fromAutoCloseable(blocker.delay(dsl.fetchLazy(rq))))
      .flatMap { cursor =>
        val iter = cursor.iterator()

        fs2.Stream.unfoldChunkEval(true) { shouldFetch =>
          if (shouldFetch) {
            blocker.delay {
              var i = 0

              val acc = collection.mutable.Buffer.newBuilder[R]
              acc.sizeHint(batch)

              while (i < batch && iter.hasNext) {
                acc += iter.next
                i += 1
              }

              (fs2.Chunk.buffer(acc.result), i == batch).some
            }
          } else {
            Sync[F].pure(Option.empty[(fs2.Chunk[R], Boolean)])
          }
        }
      }
  }

  override def execute(query: JQuery): F[Int] = {
    withDslF(_.execute(query))
  }

  protected def withDslF[T](f: DSLContext => T): F[T] = {
    blocker.delay(f(dsl))
  }
}
