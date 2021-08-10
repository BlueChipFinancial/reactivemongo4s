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
import reactivemongo.api._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.bson.BSONDocumentReader

// final class QueryBuilderWrap[P <: SerializationPack, G <: GenericCollection[P], A <: G#QueryBuilder] private (val value: A)
// object QueryBuilderWrap {
//   def apply[P <: SerializationPack, G <: GenericCollection[P]](gen: G)(queryBuiler: gen.QueryBuilder): QueryBuilderWrap[P, G, gen.QueryBuilder] =
//     new QueryBuilderWrap[P, G, gen.QueryBuilder](queryBuiler)
// }

// final class CursorProducerWrap[T] private (val cp: CursorProducer[T])
// object CursorProducerWrap {
//   implicit def toProducedCursor[T](wrap: CursorProducerWrap[T]): wrap.cp.ProducedCursor = ???
// }

object test {

  // implicit class QueryBuilderWrapOps[P <: SerializationPack, G <: GenericCollection[P], A <: G#QueryBuilder](wrap: QueryBuilderWrap[P, G, A]) {

  //   // def cursor[T](readPreference: ReadPreference = readPreference)(implicit reader: pack.Reader[T], cp: CursorProducer[T]): cp.ProducedCursor =
  //   //   cp.produce(defaultCursor[T](readPreference))
  // }\

  import cats.effect.{Async, IO}

  implicit class CursorOps[A](cursor: Cursor[A]) {
    def headF[F[_]: Async]: F[A] = ???
    def headOptionF[F[_]: Async]: F[Option[A]] = ???
  }

  def col: BSONCollection = ???

  implicit def reader: BSONDocumentReader[Int] = ???

  def testing(selector: BSONDocument): IO[Int] = {
    val cursor = col.find(selector).cursor[Int]()
    cursor.headF[IO]
  }
}

// class UserRepository[F[_]: Async](collection: BSONCollection) {

//   def findOneUser: F[Option[String]] = collection.find(???).cursor[String].headOptionF[F]
// }

final class MongoCollectionF[F[_]](
    private val collection: BSONCollection
)(implicit F: Async[F]) {
  def name: String = collection.name

  def count(selector: Option[BSONDocument] = None)(implicit ec: ExecutionContext): F[Long] =
    F.fromFutureDelay(collection.count(selector))

  def findOne[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Option[T]] =
    F.fromFutureDelay(collection.find(selector).one[T])

  def find[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext) =
    collection.find(selector).batchSize(10).cursor[T]().toStream(100)

  def watchCapped[T: Reader](selector: BSONDocument)(implicit ec: ExecutionContext): F[Stream[F, T]] =
    collection.find(selector).tailable.cursor[T]().toStreamWatch(100)

//  def watch[T: Reader](selector: BSONDocument)(implicit )

  def update[T: Writer](selector: BSONDocument, update: T, upsert: Boolean = false)(implicit ec: ExecutionContext): F[Unit] =
    F.fromFutureDelay(collection.findAndUpdate(selector, update, upsert = upsert).void)
}
