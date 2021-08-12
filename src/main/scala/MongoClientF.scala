package com.bcf

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import helpers._
import reactivemongo.api
import reactivemongo.api.{AsyncDriver, MongoConnectionOptions}

trait MongoClientF[F[_]] {
  def getDatabase(name: String): F[MongoDatabaseF[F]]
}

object MongoClientF {
  final private case class MongoClientImplF[F[_]](
      private val connection: api.MongoConnection
  )(implicit val F: Async[F], ec: ExecutionContext)
      extends MongoClientF[F] {
    override def getDatabase(name: String): F[MongoDatabaseF[F]] =
      F.fromFutureDelay(connection.database(name)).map(MongoDatabaseF[F](_))
  }

  def apply[F[_]](
      nodes: Seq[String],
      options: MongoConnectionOptions
  )(implicit F: Async[F], ec: ExecutionContext): Resource[F, MongoClientF[F]] = {
    val driver = new AsyncDriver
    Resource
      .make(F.fromFutureDelay(driver.connect(nodes, options)))(_ => F.fromFutureDelay(driver.close(10.seconds)))
      .map(c => new MongoClientImplF[F](c))
  }

}
