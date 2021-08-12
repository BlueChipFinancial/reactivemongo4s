package com.bcf

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers.AsyncExtended
import reactivemongo.api.{CollectionStats, SerializationPack}
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

    def deleteF[F[_]: Async]: DeleteOpsF[F, P] =
      DeleteOpsF(collection)

    def updateF[F[_]: Async]: UpdateOpsF[F, P] =
      UpdateOpsF(collection)

    def insertF[F[_]: Async]: InsertOpsF[F, P] =
      InsertOpsF(collection)
  }
}
