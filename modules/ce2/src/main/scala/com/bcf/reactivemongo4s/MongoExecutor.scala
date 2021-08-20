package com.bcf.reactivemongo4s

import scala.concurrent.ExecutionContext

import cats.Applicative

trait MongoExecutor[F[_]] {
  def executionContext: F[ExecutionContext]
}

object MongoExecutor {
  def apply[F[_]](implicit me: MongoExecutor[F]): MongoExecutor[F] = me
  def fromExecutionContext[F[_]: Applicative](ec: ExecutionContext): MongoExecutor[F] =
    new MongoExecutor[F] {
      override def executionContext: F[ExecutionContext] = Applicative[F].pure(ec)
    }
}
