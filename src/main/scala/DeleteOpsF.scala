package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.Collation
import reactivemongo.api.bson.BSONDocumentWriter
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult

object DeleteOpsF {
  /*
  def foo(col: BSONCollection) =
    col.delete

    (...).cursor.whaterF
                ^ here is extra F versions of method

                (...).deleteF.whatever
                (...).delete.(...)
                    ^ here is extra delete F

  def fooF[F[_]: Async](col: BSONCollection) =
    col.deleteF.oneF()
   */

  implicit final class DeleteOpsFImpl(val delete: BSONCollection#DeleteBuilder) {
    def oneF[F[_]: Async, Q: BSONDocumentWriter](
        q: Q,
        limit: Option[Int] = None,
        collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[WriteResult] =
      Async[F].fromFutureDelay(delete.one(q, limit, collation))

    def manyF[F[_]: Async](
        deletes: Iterable[BSONCollection#DeleteElement]
    )(implicit ec: ExecutionContext): F[BSONCollection#MultiBulkWriteResult] =
      ??? //Async[F].fromFutureDelay(delete.many(deletes))

    def elementF[F[_]: Async, Q: BSONDocumentWriter](
        q: Q,
        limit: Option[Int] = None,
        collation: Option[Collation] = None
    ): F[BSONCollection#DeleteElement] =
      Async[F].fromFutureDelay(delete.element(q, limit, collation))
  }

  case class DeleteFWrapper[F[_]: Async](val collection: BSONCollection) {
    def one[Q: BSONDocumentWriter](
        q: Q,
        limit: Option[Int] = None,
        collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[WriteResult] =
      Async[F].fromFutureDelay(collection.delete.one(q, limit, collation))

    def many(
        deletes: Iterable[collection.DeleteElement]
    )(implicit ec: ExecutionContext): F[collection.MultiBulkWriteResult] =
      Async[F].fromFutureDelay(collection.delete.many(deletes))

    def element[Q: BSONDocumentWriter](
        q: Q,
        limit: Option[Int] = None,
        collation: Option[Collation] = None
    ): F[collection.DeleteElement] =
      Async[F].fromFutureDelay(collection.delete.element(q, limit, collation))
  }

  implicit final class DeleteOpsFImpl3(val collection: BSONCollection) {
    def deleteF[F[_]: Async]: DeleteFWrapper[F] =
      DeleteFWrapper(collection)
  }
}
