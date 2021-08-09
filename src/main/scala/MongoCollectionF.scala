package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.Cursor.WithOps
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.collection.BSONSerializationPack.Reader
import fs2.Stream

final class MongoCollectionF[F[_]](
    private val collection: BSONCollection
)(implicit F: Async[F]) {
  def name: String = collection.name

  def count(selector: Option[BSONDocument] = None)(implicit ec: ExecutionContext): F[Long] =
    F.fromFutureDelay(collection.count(selector))

  def findOne[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Option[T]] =
    F.fromFutureDelay(collection.find(selector).one[T])

  def find[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Stream[F, T]] =
    collection.find(selector).batchSize(3).cursor[T]().toStream4(10)
}
