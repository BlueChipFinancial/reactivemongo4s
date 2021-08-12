package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.{Collation, SerializationPack}

case class UpdateOpsF[F[_] : Async, P <: SerializationPack](collection: GenericCollection[P]) {
  def elementF[Q: collection.pack.Writer, U: collection.pack.Writer](
    q: Q,
    u: U,
    upsert: Boolean = false,
    multi: Boolean = false,
    collation: Option[Collation] = None
  ): F[collection.UpdateElement] =
    Async[F].fromFutureDelay(collection.update.element(q, u, upsert, multi, collation))

  def oneF[Q: collection.pack.Writer, U: collection.pack.Writer](
    q: Q,
    u: U,
    upsert: Boolean = false,
    multi: Boolean = false,
    collation: Option[Collation] = None
  )(implicit ec: ExecutionContext): F[collection.UpdateWriteResult] =
    Async[F].fromFutureDelay(collection.update.one(q, u, upsert, multi, collation))

  def manyF(
    updates: Iterable[collection.UpdateElement]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(collection.update.many(updates))
}
