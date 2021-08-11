package com.bcf

import scala.concurrent.Future

import cats.effect.Async

object helpers {

  implicit final class AsyncExtended[F[_]](private val F: Async[F]) extends AnyVal {
    def fromFutureDelay[A](thunk: => Future[A]): F[A] = F.fromFuture(F.delay(thunk))
  }
}
