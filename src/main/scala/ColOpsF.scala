package com.bcf

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers.AsyncExtended
import reactivemongo.api.{CollectionStats, SerializationPack, WriteConcern}
import reactivemongo.api.collections.GenericCollection

object ColOpsF {
  implicit final class GenColExt[P <: SerializationPack](val collection: GenericCollection[P]) extends AnyVal {
    def countF[F[_]: Async](implicit ec: ExecutionContext): F[Long] =
      Async[F].fromFutureDelay(collection.count())

    def createF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.create())

    def dropF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.drop())

    def statsF[F[_]: Async](implicit ec: ExecutionContext): F[CollectionStats] =
      Async[F].fromFutureDelay(collection.stats())

    def deleteF[F[_]: Async](
      ordered: Boolean = false,
      writeConcern: Option[WriteConcern] = None
    ): DeleteOpsF[F, P] =
      DeleteOpsF(collection, ordered, writeConcern)

    def updateF[F[_]: Async](
      ordered: Boolean = false,
      writeConcern: Option[WriteConcern] = None,
      bypassDocumentValidation: Boolean = false
    ): UpdateOpsF[F, P] =
      UpdateOpsF(collection, ordered, writeConcern, bypassDocumentValidation)

    def insertF[F[_]: Async](
      ordered: Boolean = false,
      writeConcern: Option[WriteConcern] = None,
      bypassDocumentValidation: Boolean = false
    ): InsertOpsF[F, P] =
      InsertOpsF(collection, ordered, writeConcern, bypassDocumentValidation)
  }
}
