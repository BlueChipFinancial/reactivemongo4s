package com.bcf.reactivemongo4s

import scala.collection.Factory
import scala.concurrent.duration.FiniteDuration

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api._
import reactivemongo.api.collections.GenericCollection

trait CollectionOpsF {
  implicit final class GenColExt[P <: SerializationPack](val collection: GenericCollection[P]) {
    def countF[F[_]: Async: MongoExecutor]: F[Long] =
      Async[F].fromFutureDelay(collection.count()(_))

    def createF[F[_]: Async: MongoExecutor]: F[Unit] =
      Async[F].fromFutureDelay(collection.create()(_))

    def dropF[F[_]: Async: MongoExecutor]: F[Unit] =
      Async[F].fromFutureDelay(collection.drop()(_))

    def statsF[F[_]: Async: MongoExecutor]: F[CollectionStats] =
      Async[F].fromFutureDelay(collection.stats()(_))

    def convertToCappedF[F[_]: Async: MongoExecutor](
        size: Long,
        maxDocuments: Option[Int]
    ): F[Unit] =
      Async[F].fromFutureDelay(collection.convertToCapped(size, maxDocuments)(_))

    def createCappedF[F[_]: Async: MongoExecutor](
        size: Long,
        maxDocuments: Option[Int]
    ): F[Unit] =
      Async[F].fromFutureDelay(collection.createCapped(size, maxDocuments)(_))

    def createViewF[F[_]: Async: MongoExecutor](
        name: String,
        operator: collection.PipelineOperator,
        pipeline: Seq[collection.PipelineOperator],
        collation: Option[Collation] = None
    ): F[Unit] =
      Async[F].fromFutureDelay(collection.createView(name, operator, pipeline, collation)(_))

    def distinctF[F[_]: Async: MongoExecutor, T, M[_] <: Iterable[_]](
        key: String,
        selector: Option[collection.pack.Document] = None,
        readConcern: ReadConcern = ReadConcern.Local,
        collation: Option[Collation] = None
    )(implicit
        reader: collection.pack.NarrowValueReader[T],
        cbf: Factory[T, M[T]]
    ): F[M[T]] =
      Async[F].fromFutureDelay(collection.distinct(key, selector, readConcern, collation)(reader, _, cbf))

    def findAndModifyF[F[_]: Async: MongoExecutor, S](
        selector: S,
        modifier: collection.FindAndModifyOp,
        sort: Option[collection.pack.Document] = None,
        fields: Option[collection.pack.Document] = None,
        bypassDocumentValidation: Boolean = false,
        writeConcern: WriteConcern = WriteConcern.Acknowledged,
        maxTime: Option[FiniteDuration] = None,
        collation: Option[Collation] = None,
        arrayFilters: Seq[collection.pack.Document] = Seq.empty
    )(implicit swriter: collection.pack.Writer[S]): F[collection.FindAndModifyResult] =
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
        )(swriter, _)
      )

    def findAndRemove[F[_]: Async: MongoExecutor, S](
        selector: S,
        sort: Option[collection.pack.Document] = None,
        fields: Option[collection.pack.Document] = None,
        writeConcern: WriteConcern = WriteConcern.Acknowledged,
        maxTime: Option[FiniteDuration] = None,
        collation: Option[Collation] = None,
        arrayFilters: Seq[collection.pack.Document] = Seq.empty
    )(implicit swriter: collection.pack.Writer[S]): F[collection.FindAndModifyResult] =
      Async[F].fromFutureDelay(
        collection.findAndRemove(
          selector,
          sort,
          fields,
          writeConcern,
          maxTime,
          collation,
          arrayFilters
        )(swriter, _)
      )

    def findAndUpdate[F[_]: Async: MongoExecutor, S, T](
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
        writer: collection.pack.Writer[T]
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
        )(swriter, writer, _)
      )

    def deleteF[F[_]: Async: MongoExecutor](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None
    ): DeleteOpsF[F, P] =
      DeleteOpsF(collection, ordered, writeConcern)

    def updateF[F[_]: Async: MongoExecutor](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None,
        bypassDocumentValidation: Boolean = false
    ): UpdateOpsF[F, P] =
      UpdateOpsF(collection, ordered, writeConcern, bypassDocumentValidation)

    def insertF[F[_]: Async: MongoExecutor](
        ordered: Boolean = false,
        writeConcern: Option[WriteConcern] = None,
        bypassDocumentValidation: Boolean = false
    ): InsertOpsF[F, P] =
      InsertOpsF(collection, ordered, writeConcern, bypassDocumentValidation)
  }
}
