package com.bcf.reactivemongo4s

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.{Collation, SerializationPack, WriteConcern}
import reactivemongo.core.protocol.ProtocolMetadata

case class UpdateOpsF[F[_]: Async: MongoExecutor, P <: SerializationPack](
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

  def elementF[Q, U](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
  )(implicit qw: collection.pack.Writer[Q], uw: collection.pack.Writer[U]): F[collection.UpdateElement] =
    Async[F].fromFutureDelay(_ => builder.element(q, u, upsert, multi, collation)(qw, uw))

  def oneF[Q, U](
      q: Q,
      u: U,
      upsert: Boolean = false,
      multi: Boolean = false,
      collation: Option[Collation] = None
  )(implicit qw: collection.pack.Writer[Q], uw: collection.pack.Writer[U]): F[collection.UpdateWriteResult] =
    Async[F].fromFutureDelay(builder.one(q, u, upsert, multi, collation)(_, qw, uw))

  def manyF(
      updates: Iterable[collection.UpdateElement]
  ): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(updates)(_))
}
