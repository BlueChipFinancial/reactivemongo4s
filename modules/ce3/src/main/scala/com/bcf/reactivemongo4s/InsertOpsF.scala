package com.bcf.reactivemongo4s

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{SerializationPack, WriteConcern}

case class InsertOpsF[F[_]: Async, P <: SerializationPack](
    collection: GenericCollection[P],
    ordered: Boolean = false,
    writeConcern: Option[WriteConcern] = None,
    bypassDocumentValidation: Boolean = false
) {
  private def builder =
    writeConcern match {
      case Some(concern) => collection.insert(ordered, concern, bypassDocumentValidation)
      case None          => collection.insert(ordered, bypassDocumentValidation)
    }

  def one[T](
      document: T
  )(implicit writer: collection.pack.Writer[T]): F[WriteResult] =
    Async[F].fromFutureDelay(builder.one(document)(_, writer))

  def many[T](
      documents: Iterable[T]
  )(implicit writer: collection.pack.Writer[T]): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(documents)(_, writer))
}
