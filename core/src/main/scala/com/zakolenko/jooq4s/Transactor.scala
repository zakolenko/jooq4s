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

  def stream[R <: Record](rq: JResultQuery[R], batch: Int): fs2.Stream[F, R]

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