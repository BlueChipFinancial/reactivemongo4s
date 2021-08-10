package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import cats.implicits._
import fs2.Stream
import helpers._
import reactivemongo.api.Cursor.WithOps
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.collection.BSONSerializationPack.{Reader, Writer, serialize}

final class MongoCollectionF[F[_]](
    private val collection: BSONCollection
)(implicit F: Async[F]) {
  def name: String = collection.name

  def count(selector: Option[BSONDocument] = None)(implicit ec: ExecutionContext): F[Long] =
    F.fromFutureDelay(collection.count(selector))

  def findOne[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Option[T]] =
    F.fromFutureDelay(collection.find(selector).one[T])

  def find[T: Reader](selector: BSONDocument, qbO: Option[QueryBuilder] = None)(implicit ec: ExecutionContext) =
    collection.find(selector).batchSize(10).cursor[T]().toStream(100)

  def watchCapped[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Stream[F, T]] =
    collection.find(selector).tailable.cursor[T]().toStreamWatch(100)

//  def watch[T: Reader](selector: BSONDocument)(implicit )

  def update[T: Writer](selector: BSONDocument, update: T, upsert: Boolean = false)
    (implicit ec: ExecutionContext): F[Unit] = {
    F.fromFutureDelay(collection.findAndUpdate(selector, update, upsert = upsert).void)
  }
}
