package com.zakolenko.jooq4s

import org.jooq.Query

object Query {

  final class Ops(private val query: Query) extends AnyVal {
    def execute[F[_]](tr: Transactor[F]): F[Int] = tr.execute(query)
  }

  trait ToQueryOps {
    implicit def toQueryOps(query: Query): Query.Ops = new Query.Ops(query)
  }
}