package com.bcf.reactivemongo4s

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Collation, SerializationPack, WriteConcern}

case class DeleteOpsF[F[_]: Async, P <: SerializationPack](
    collection: GenericCollection[P],
    ordered: Boolean = false,
    writeConcern: Option[WriteConcern] = None
) {
  private def builder =
    writeConcern match {
      case Some(concern) => collection.delete(ordered, concern)
      case None          => collection.delete(ordered)
    }

  def one[Q: collection.pack.Writer](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  )(implicit ec: ExecutionContext): F[WriteResult] =
    Async[F].fromFutureDelay(builder.one(q, limit, collation))

  def many(
      deletes: Iterable[collection.DeleteElement]
  )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(deletes))

  def element[Q: collection.pack.Writer](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  ): F[collection.DeleteElement] =
    Async[F].fromFutureDelay(builder.element(q, limit, collation))
}
