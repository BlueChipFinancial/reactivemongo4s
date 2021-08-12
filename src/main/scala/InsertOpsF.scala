package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.{SerializationPack, WriteConcern}
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult


case class InsertOpsF[F[_] : Async, P <: SerializationPack](
  collection: GenericCollection[P],
  ordered: Boolean = false,
  writeConcern: Option[WriteConcern] = None,
  bypassDocumentValidation: Boolean = false
) {
  private def builder = writeConcern match {
    case Some(concern) => collection.insert(ordered, concern, bypassDocumentValidation)
    case None => collection.insert(ordered, bypassDocumentValidation)
  }

  def one[T: collection.pack.Writer](
    document: T
  )(implicit ec: ExecutionContext): F[WriteResult] =
    Async[F].fromFutureDelay(builder.one(document))

  def many[T: collection.pack.Writer](
    documents: Iterable[T]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(documents))
}
