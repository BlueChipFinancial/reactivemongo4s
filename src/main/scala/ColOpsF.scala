package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers.AsyncExtended
import reactivemongo.api.{CollectionStats, SerializationPack}
import reactivemongo.api.collections.GenericCollection

object ColOpsF {
  implicit final class GenColExt[P <: SerializationPack](val col: GenericCollection[P]) extends AnyVal {
    def countF[F[_]: Async](implicit ec: ExecutionContext): F[Long] =
      Async[F].fromFutureDelay(col.count())

    def createF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(col.create())

    def dropF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(col.drop())

    def statsF[F[_]: Async](implicit ec: ExecutionContext): F[CollectionStats] =
      Async[F].fromFutureDelay(col.stats())

  }
}
