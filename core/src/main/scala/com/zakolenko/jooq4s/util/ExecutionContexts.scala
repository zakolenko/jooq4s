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
