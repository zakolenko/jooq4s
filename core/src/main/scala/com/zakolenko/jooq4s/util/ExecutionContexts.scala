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

package com.zakolenko.jooq4s.util

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{Resource, Sync}

import scala.concurrent.ExecutionContext

object ExecutionContexts {

  def cached[F[_]: Sync]: Resource[F, ExecutionContext] = {
    fromExecutorService(Executors.newCachedThreadPool())
  }

  def fixed[F[_]: Sync](size: Int): Resource[F, ExecutionContext] = {
    fromExecutorService(Executors.newFixedThreadPool(size))
  }

  private def fromExecutorService[F[_]: Sync](es: => ExecutorService): Resource[F, ExecutionContext] = {
    Resources
      .make(es)(_.shutdown())
      .map(ExecutionContext.fromExecutorService)
  }
}
