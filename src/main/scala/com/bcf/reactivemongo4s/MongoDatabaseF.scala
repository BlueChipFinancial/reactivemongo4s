package com.bcf.reactivemongo4s

import cats.effect.Async
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api.DB
import reactivemongo.api.bson.collection.BSONCollection

trait MongoDatabaseF[F[_]] {
  def name: String
  def getCollection(name: String): F[BSONCollection]
  def collectionNames: F[List[String]]
  def createCollection(name: String): F[Unit]
}

object MongoDatabaseF {
  final private class LiveMongoDatabaseF[F[_]](
      private val database: DB
  )(implicit
      val F: Async[F]
  ) extends MongoDatabaseF[F] {
    def name: String = database.name

    override def getCollection(name: String): F[BSONCollection] =
      F.delay(database.collection[BSONCollection](name))

    override def collectionNames: F[List[String]] =
      F.fromFutureDelay(database.collectionNames(_))

    override def createCollection(name: String): F[Unit] =
      F.fromFutureDelay(database.collection[BSONCollection](name).create()(_))
  }

  def apply[F[_]: Async](database: DB): MongoDatabaseF[F] = new LiveMongoDatabaseF[F](database)
}
