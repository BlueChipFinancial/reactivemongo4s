package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Collation, SerializationPack}

case class DeleteOpsF[F[_]: Async, P <: SerializationPack](collection: GenericCollection[P]) {
  def one[Q: collection.pack.Writer](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  )(implicit ec: ExecutionContext): F[WriteResult] =
    Async[F].fromFutureDelay(collection.delete.one(q, limit, collation))

  def many(
      deletes: Iterable[collection.DeleteElement]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(collection.delete.many(deletes))

  def element[Q: collection.pack.Writer](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  ): F[collection.DeleteElement] =
    Async[F].fromFutureDelay(collection.delete.element(q, limit, collation))
}
