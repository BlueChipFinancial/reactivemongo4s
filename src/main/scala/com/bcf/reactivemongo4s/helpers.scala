package com.bcf.reactivemongo4s

import scala.concurrent.{ExecutionContext, Future}

import cats.Monad
import cats.effect.Async

object helpers {

  implicit final class AsyncExtended[F[_]: Async](private val F: Async[F]) {
    def fromFutureDelay[A](thunk: ExecutionContext => Future[A]): F[A] =
      Monad[F].flatMap(F.executionContext)(thunk andThen F.pure andThen F.fromFuture[A])
  }
}
