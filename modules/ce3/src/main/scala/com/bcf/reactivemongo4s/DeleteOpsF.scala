package com.bcf.reactivemongo4s

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
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

  def one[Q](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  )(implicit qw: collection.pack.Writer[Q]): F[WriteResult] =
    Async[F].fromFutureDelay(builder.one(q, limit, collation)(_, qw))

  def many(
      deletes: Iterable[collection.DeleteElement]
  ): F[collection.MultiBulkWriteResult] =
    Async[F].fromFutureDelay(builder.many(deletes)(_))

  def element[Q](
      q: Q,
      limit: Option[Int] = None,
      collation: Option[Collation] = None
  )(implicit qw: collection.pack.Writer[Q]): F[collection.DeleteElement] =
    Async[F].fromFutureDelay(_ => builder.element(q, limit, collation)(qw))
}
