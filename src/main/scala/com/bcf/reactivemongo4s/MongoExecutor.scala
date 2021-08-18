package com.bcf.reactivemongo4s

import scala.concurrent.ExecutionContext

trait MongoExecutor[F[_]] {
  def executionContext: F[ExecutionContext]
}

object MongoExecutor {
  def apply[F[_]](implicit me: MongoExecutor[F]): MongoExecutor[F] = me
}
