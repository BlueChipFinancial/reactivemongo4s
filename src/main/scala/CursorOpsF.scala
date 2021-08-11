package com.bcf

import scala.collection.Factory
import scala.concurrent.{ExecutionContext, Future}

import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Deferred, Sync}
import cats.implicits._
import fs2.{Chunk, Stream}
import helpers._
import reactivemongo.api.Cursor
import reactivemongo.api.Cursor.ErrorHandler

object CursorOpsF {
  implicit final class CursorOpsFImpl[T](val cursor: Cursor.WithOps[T]) extends AnyVal {
    def headOptionF[F[_]: Async](implicit ec: ExecutionContext): F[Option[T]] =
      Async[F].fromFutureDelay(cursor.headOption)

    def collectF[F[_]: Async, M[_]](maxDocs: Int = Int.MaxValue, err: ErrorHandler[M[T]] = Cursor.FailOnError[M[T]]())(
      implicit cbf: Factory[T, M[T]], ec: ExecutionContext
    ): F[M[T]] = Async[F].fromFutureDelay(cursor.collect(maxDocs, err))

    def peekF[F[_]: Async, M[_]](maxDocs: Int)(
      implicit cbf: Factory[T, M[T]], ec: ExecutionContext
    ): F[Cursor.Result[M[T]]] = Async[F].fromFutureDelay(cursor.peek(maxDocs))

    def toStream[F[_]: Async](capacity: Int)(implicit ec: ExecutionContext): F[Stream[F, T]] = {
      Sync[F].delay(
        for {
          dispatcher <- Stream.resource(Dispatcher[F])
          queue <- Stream.eval(Queue.bounded[F, Option[Chunk[T]]](capacity))
          promise <- Stream.eval(Deferred[F, Unit])
          _ <- Stream.bracket {
            Async[F].fromFutureDelay {
              def enqueue(v: Option[Chunk[T]]): Future[Either[Unit, Unit]] =
                dispatcher.unsafeToFuture(Async[F].race(promise.get, queue.offer(v)))

              cursor
                .foldBulksM(()) { (_, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  enqueue(chunk.some).flatMap {
                    case Left(_) =>
                      enqueue(None).map(_ => Cursor.Done())
                    case Right(_) =>
                      Future.successful(Cursor.Cont())
                  }
                }.flatMap(_ => enqueue(None))

              Future.successful(promise)
            }
          }(_.complete(()).void)
          stream <- Stream.fromQueueNoneTerminatedChunk(queue)
        } yield stream
      )
    }

    def toStreamUnterminated[F[_]: Async](capacity: Int)(implicit ec: ExecutionContext): F[Stream[F, T]] = {
      Sync[F].delay(
        for {
          dispatcher <- Stream.resource(Dispatcher[F])
          queue <- Stream.eval(Queue.bounded[F, Chunk[T]](capacity))
          promise <- Stream.eval(Deferred[F, Unit])
          _ <- Stream.bracket {
            Async[F].fromFutureDelay {
              def enqueue(v: Chunk[T]): Future[Either[Unit, Unit]] =
                dispatcher.unsafeToFuture(Async[F].race(promise.get, queue.offer(v)))

              cursor
                .foldBulksM(()) { (_, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  if (chunk.isEmpty && cursor.tailable) {
                    Future.successful(Cursor.Cont(()))
                  } else {
                    enqueue(chunk).flatMap {
                      case Left(_) =>
                        Future.successful(Cursor.Done())
                      case Right(_) =>
                        Future.successful(Cursor.Cont())
                    }
                  }
                }
              Future.successful(promise)
            }
          }(_.complete(()).void)
          stream <- Stream.fromQueueUnterminatedChunk(queue)
        } yield stream
      )
    }
  }
}
