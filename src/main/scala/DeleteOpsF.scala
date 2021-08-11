package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.Collation
import reactivemongo.api.bson.BSONDocumentWriter
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult

object DeleteOpsF {
  implicit final class DeleteOpsFImpl(val delete: BSONCollection#DeleteBuilder) {
    def oneF[F[_]: Async, Q: BSONDocumentWriter](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[WriteResult] =
      Async[F].fromFutureDelay(delete.one(q, limit, collation))

    def manyF[F[_]: Async](
      deletes: Iterable[BSONCollection#DeleteElement]
    )(implicit ec: ExecutionContext): F[BSONCollection#MultiBulkWriteResult] =
      Async[F].fromFutureDelay(delete.many(deletes))

    def elementF[F[_]: Async, Q: BSONDocumentWriter](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
    ): F[BSONCollection#DeleteElement] =
      Async[F].fromFutureDelay(delete.element(q, limit, collation))
  }
}