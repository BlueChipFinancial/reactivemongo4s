package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.collections.{DeleteOps, GenericCollection}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Collation, SerializationPack}

object DeleteOpsF {
  implicit final class DeleteOpsFImpl[P <: SerializationPack](val delete: GenericCollection[P]#DeleteBuilder) {
    def oneF[F[_]: Async, Q, U](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[WriteResult] =
      Async[F].fromFutureDelay(delete.one(q, limit, collation)(ec, w))
  }
}
