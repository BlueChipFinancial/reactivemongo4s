package com.bcf.reactivemongo4s

import scala.collection.Factory
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api._
import reactivemongo.api.collections.GenericCollection

trait CollectionOpsF {
  implicit final class GenColExt[P <: SerializationPack](val collection: GenericCollection[P]) {
    def countF[F[_]: Async](implicit ec: ExecutionContext): F[Long] =
      Async[F].fromFutureDelay(collection.count())

    def createF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.create())

    def dropF[F[_]: Async](implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.drop())

    def statsF[F[_]: Async](implicit ec: ExecutionContext): F[CollectionStats] =
      Async[F].fromFutureDelay(collection.stats())

    def convertToCappedF[F[_]: Async](
        size: Long,
        maxDocuments: Option[Int]
    )(implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.convertToCapped(size, maxDocuments))

    def createCappedF[F[_]: Async](
        size: Long,
        maxDocuments: Option[Int]
    )(implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.createCapped(size, maxDocuments))

    def createViewF[F[_]: Async](
        name: String,
        operator: collection.PipelineOperator,
        pipeline: Seq[collection.PipelineOperator],
        collation: Option[Collation] = None
    )(implicit ec: ExecutionContext): F[Unit] =
      Async[F].fromFutureDelay(collection.createView(name, operator, pipeline, collation))

    def distinctF[F[_]: Async, T, M[_] <: Iterable[_]](
        key: String,
        selector: Option[collection.pack.Document] = None,
        readConcern: ReadConcern = ReadConcern.Local,
        collation: Option[Collation] = None
    )(implicit
        reader: collection.pack.NarrowValueReader[T],
        ec: ExecutionContext,
        cbf: Factory[T, M[T]]
    ): F[M[T]] =
      Async[F].fromFutureDelay(collection.distinct(key, selector, readConcern, collation))

    def findAndModifyF[F[_]: Async, S](
        selector: S,
        modifier: collection.FindAndModifyOp,
        sort: Option[collection.pack.Document] = None,
        fields: Option[collection.pack.Document] = None,
        bypassDocumentValidation: Boolean = false,
        writeConcern: WriteConcern = WriteConcern.Acknowledged,
        maxTime: Option[FiniteDuration] = None,
        collation: Option[Collation] = None,
        arrayFilters: Seq[collection.pack.Document] = Seq.empty
    )(implicit swriter: collection.pack.Writer[S], ec: ExecutionContext): F[collection.FindAndModifyResult] =
      Async[F].fromFutureDelay(
        collection.findAndModify(
          selector,
          modifier,
          sort,
          fields,
          bypassDocumentValidation,
          writeConcern,
          maxTime,
          collation,
          arrayFilters
        )
      )

    def findAndRemove[F[_]: Async, S](
        selector: S,
        sort: Option[collection.pack.Document] = None,
        fields: Option[collection.pack.Document] = None,
        writeConcern: WriteConcern = WriteConcern.Acknowledged,
        maxTime: Option[FiniteDuration] = None,
        collation: Option[Collation] = None,
        arrayFilters: Seq[collection.pack.Document] = Seq.empty
    )(implicit swriter: collection.pack.Writer[S], ec: ExecutionContext): F[collection.FindAndModifyResult] =
      Async[F].fromFutureDelay(
        collection.findAndRemove(
          selector,
          sort,
          fields,
          writeConcern,
          maxTime,
          collation,
          arrayFilters
        )
      )

    def findAndUpdate[F[_]: Async, S, T](
        selector: S,
        update: T,
        fetchNewObject: Boolean = false,
        upsert: Boolean = false,
        sort: Option[collection.pack.Document] = None,
        fields: Option[collection.pack.Document] = None,
        bypassDocumentValidation: Boolean = false,
        writeConcern: WriteConcern = WriteConcern.Acknowledged,
        maxTime: Option[FiniteDuration] = None,
        collation: Option[Collation] = None,
        arrayFilters: Seq[collection.pack.Document] = Seq.empty
    )(implicit
        swriter: collection.pack.Writer[S],
        writer: collection.pack.Writer[T],
        ec: ExecutionContext
    ): F[collection.FindAndModifyResult] =
      Async[F].fromFutureDelay(
        collection.findAndUpdate(
          selector,
          update,
          fetchNewObject,
          upsert,
          sort,
          fields,
          bypassDocumentValidation,
          writeConcern,
          maxTime,
          collation,
          arrayFilters
        )
      )

    def deleteF[F[_]: Async](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None
    ): DeleteOpsF[F, P] =
      DeleteOpsF(collection, ordered, writeConcern)

    def updateF[F[_]: Async](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None,
        bypassDocumentValidation: Boolean = false
    ): UpdateOpsF[F, P] =
      UpdateOpsF(collection, ordered, writeConcern, bypassDocumentValidation)

    def insertF[F[_]: Async](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None,
        bypassDocumentValidation: Boolean = false
    ): InsertOpsF[F, P] =
      InsertOpsF(collection, ordered, writeConcern, bypassDocumentValidation)
  }
}
