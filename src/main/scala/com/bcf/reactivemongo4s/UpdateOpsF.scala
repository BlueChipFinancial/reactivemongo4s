package com.bcf.reactivemongo4s

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.{Collation, SerializationPack, WriteConcern}
import reactivemongo.core.protocol.ProtocolMetadata

case class UpdateOpsF[F[_]: Async, P <: SerializationPack](
    collection: GenericCollection[P],
    ordered: Boolean = false,
    writeConcern: Option[WriteConcern] = None,
    bypassDocumentValidation: Boolean = false,
    maxBulkSize: Int = ProtocolMetadata.Default.maxBulkSize
) {
  private def builder =
    writeConcern match {
      case Some(concern) => collection.update(ordered, concern, bypassDocumentValidation).maxBulkSize(maxBulkSize)
      case None          => collection.update(ordered, bypassDocumentValidation).maxBulkSize(maxBulkSize)
    }

  def elementF[Q: collection.pack.Writer, U: collection.pack.Writer](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
  ): F[collection.UpdateElement] =
    Async[F].fromFutureDelay(builder.element(q, u, upsert, multi, collation))

  def oneF[Q: collection.pack.Writer, U: collection.pack.Writer](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
  )(implicit ec: ExecutionContext): F[collection.UpdateWriteResult] =
    Async[F].fromFutureDelay(builder.one(q, u, upsert, multi, collation))

  def manyF(
      updates: Iterable[collection.UpdateElement]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(updates))
}
