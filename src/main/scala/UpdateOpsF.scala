package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import reactivemongo.api.Collation
import reactivemongo.api.bson.BSONDocumentWriter
import reactivemongo.api.bson.collection.BSONCollection
import helpers._

object UpdateOpsF {
  implicit final class UpdateOpsFImpl(val update: BSONCollection#UpdateBuilder) extends AnyVal {
    def elementF[F[_]: Async, Q: BSONDocumentWriter, U: BSONDocumentWriter](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
    ): F[BSONCollection#UpdateElement] =
      Async[F].fromFutureDelay(update.element(q, u, upsert, multi, collation))

    def oneF[F[_]: Async, Q: BSONDocumentWriter, U: BSONDocumentWriter](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[BSONCollection#UpdateWriteResult] =
      Async[F].fromFutureDelay(update.one(q, u, upsert, multi, collation))

    def manyF[F[_]: Async](
      updates: Iterable[BSONCollection#UpdateElement]
    )(implicit ec: ExecutionContext): F[BSONCollection#MultiBulkWriteResult] =
      Async[F].fromFutureDelay(update.many(updates))
  }
}
