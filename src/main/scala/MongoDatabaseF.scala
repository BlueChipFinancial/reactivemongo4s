package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.Async
import helpers._
import reactivemongo.api.DB
import reactivemongo.api.bson.collection.BSONCollection

trait MongoDatabaseF[F[_]] {
  def name: String
  def getCollection(name: String): F[MongoCollectionF[F]]
  def collectionNames(implicit ec: ExecutionContext): F[List[String]]
  def createCollection(name: String)(implicit ec: ExecutionContext): F[Unit]
}

object MongoDatabaseF {
  final private class LiveMongoDatabaseF[F[_]](
    private val database: DB
  )(implicit
    val F: Async[F]
  ) extends MongoDatabaseF[F] {
    def name: String = database.name

    override def getCollection(name: String): F[MongoCollectionF[F]] =
      F.delay(new MongoCollectionF[F](database.collection[BSONCollection](name)))

    override def collectionNames(implicit ec: ExecutionContext): F[List[String]] =
      F.fromFutureDelay(database.collectionNames)

    override def createCollection(name: String)(implicit ec: ExecutionContext): F[Unit] =
      F.fromFutureDelay(database.collection[BSONCollection](name).create())
  }

  def apply[F[_]: Async](database: DB): MongoDatabaseF[F] = new LiveMongoDatabaseF[F](database)
}
