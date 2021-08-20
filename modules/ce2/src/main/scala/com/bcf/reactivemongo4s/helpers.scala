package com.bcf.reactivemongo4s

import scala.concurrent.{ExecutionContext, Future}

import cats.Monad
import cats.effect.Async

object helpers {

  implicit final class AsyncExtended[F[_]: Async: MongoExecutor](private val F: Async[F]) {
    private def fromFutureDelay_[A](thunk: Future[A])(ec: ExecutionContext): F[A] =
      F.async { cb =>
        import scala.util.{Failure, Success}

        thunk.onComplete {
          case Failure(error) => cb(Left(error))
          case Success(value) => cb(Right(value))
        }(ec)
      }

    def fromFutureDelay[A](thunk: ExecutionContext => Future[A]): F[A] =
      Monad[F].flatMap(MongoExecutor[F].executionContext)(ec => fromFutureDelay_(thunk(ec))(ec))
  }
}
