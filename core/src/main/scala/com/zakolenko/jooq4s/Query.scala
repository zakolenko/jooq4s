package com.zakolenko.jooq4s

import org.jooq.{Query => JQuery}

object Query {

  trait ToQueryOps {
    implicit def toQueryOps(query: JQuery): Query.Ops = new Query.Ops(query)
  }

  final class Ops(private val query: JQuery) extends AnyVal {
    def execute[F[_]](tr: Transactor[F]): F[Int] = tr.execute(query)
  }
}