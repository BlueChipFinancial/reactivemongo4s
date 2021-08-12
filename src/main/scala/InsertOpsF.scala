package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.SerializationPack
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult

case class InsertOpsF[F[_]: Async, P <: SerializationPack](collection: GenericCollection[P]) {
  def one[T: collection.pack.Writer](
      document: T
  )(implicit ec: ExecutionContext): F[WriteResult] =
    Async[F].fromFutureDelay(collection.insert.one(document))

  def many[T: collection.pack.Writer](
      documents: Iterable[T]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(collection.insert.many(documents))
}
