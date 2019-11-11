package com.zakolenko.jooq4s.util

import cats.effect.{Resource, Sync}

object Resources {

  def make[F[_]: Sync, T](acquire: => T)(release: T => Unit): Resource[F, T] = {
    Resource.make(Sync[F].delay(acquire))(t => Sync[F].delay(release(t)))
  }
}
