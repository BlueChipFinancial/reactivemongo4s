package com.bcf.reactivemongo4s

import scala.concurrent.duration._

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.bcf.reactivemongo4s.helpers._
import reactivemongo.api
import reactivemongo.api.{AsyncDriver, FailoverStrategy, MongoConnectionOptions}

trait MongoClientF[F[_]] {
  def getDatabase(name: String, failoverStrategy: FailoverStrategy = FailoverStrategy()): F[MongoDatabaseF[F]]
  def auth(db: String, user: String, password: String, failoverStrategy: FailoverStrategy = FailoverStrategy()): F[Unit]
}

object MongoClientF {
  final private case class MongoClientImplF[F[_]: MongoExecutor](
      private val connection: api.MongoConnection
  )(implicit val F: Async[F])
      extends MongoClientF[F] {
    override def getDatabase(name: String, failoverStrategy: FailoverStrategy = FailoverStrategy()): F[MongoDatabaseF[F]] =
      F.fromFutureDelay(connection.database(name)(_)).map(MongoDatabaseF[F](_))

    override def auth(db: String, user: String, password: String, failoverStrategy: FailoverStrategy = FailoverStrategy()): F[Unit] =
      F.fromFutureDelay(connection.authenticate(db, user, password, failoverStrategy)(_)).void
  }

  def apply[F[_]: MongoExecutor](
      nodes: Seq[String],
      options: MongoConnectionOptions,
      closeTimeout: FiniteDuration = 10.seconds
  )(implicit F: Async[F]): Resource[F, MongoClientF[F]] = {
    val driver = new AsyncDriver
    Resource
      .make(F.fromFutureDelay(_ => driver.connect(nodes, options)))(_ => F.fromFutureDelay(driver.close(closeTimeout)(_)))
      .map(c => new MongoClientImplF[F](c))
  }

}
