package com.bcf.reactivemongo4s

import scala.collection.Factory
import scala.concurrent.Future

import cats.effect.concurrent.Deferred
import cats.effect.{Async, Concurrent, ConcurrentEffect, Sync}
import cats.implicits._
import com.bcf.reactivemongo4s.helpers._
import fs2.concurrent.Queue
import fs2.{Chunk, Stream}
import reactivemongo.api.Cursor
import reactivemongo.api.Cursor.ErrorHandler

trait CursorOpsF {
  implicit final class CursorOpsFImpl[T](val cursor: Cursor.WithOps[T]) {
    def headOptionF[F[_]: Async: MongoExecutor]: F[Option[T]] =
      Async[F].fromFutureDelay(cursor.headOption(_))

    def headF[F[_]: Async: MongoExecutor]: F[T] =
      Async[F].fromFutureDelay(cursor.head(_))

    def collectF[F[_]: Async: MongoExecutor, M[_]](maxDocs: Int = Int.MaxValue, err: ErrorHandler[M[T]] = Cursor.FailOnError[M[T]]())(implicit
        cbf: Factory[T, M[T]]
    ): F[M[T]] = Async[F].fromFutureDelay(cursor.collect(maxDocs, err)(cbf, _))

    def peekF[F[_]: Async: MongoExecutor, M[_]](maxDocs: Int)(implicit
        cbf: Factory[T, M[T]]
    ): F[Cursor.Result[M[T]]] = Async[F].fromFutureDelay(cursor.peek(maxDocs)(cbf, _))

    private def enqueueFuture[F[_]: ConcurrentEffect, A](queue: Queue[F, A], promise: Deferred[F, _])(el: A) =
      ConcurrentEffect[F].toIO(Concurrent[F].race(promise.get, queue.enqueue(Stream(el)).compile.drain)).unsafeToFuture()

    def toStream[F[_]: ConcurrentEffect: MongoExecutor](queueCapacity: Int): F[Stream[F, T]] =
      Sync[F].delay(
        for {
          queue <- Stream.eval(Queue.bounded[F, Option[Chunk[T]]](queueCapacity))
          promise <- Stream.eval(Deferred[F, Unit])
          _ <- Stream.bracket {
            Async[F].fromFutureDelay { implicit ec =>
              val enqueue = enqueueFuture[F, Option[Chunk[T]]](queue, promise) _

              cursor
                .foldBulksM(()) { (_, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  enqueue(chunk.some).flatMap {
                    case Left(_) =>
                      enqueue(None).map(_ => Cursor.Done())
                    case Right(_) =>
                      Future.successful(Cursor.Cont())
                  }
                }
                .flatMap(_ => enqueue(None))

              Future.successful(promise)
            }
          }(_.complete(()).void)
          stream <- StreamHelpers.fromQueueNoneTerminatedChunk(queue)
        } yield stream
      )

    def toStreamUnterminated[F[_]: ConcurrentEffect: MongoExecutor](capacity: Int): F[Stream[F, T]] =
      Sync[F].delay(
        for {
          queue <- Stream.eval(Queue.bounded[F, Chunk[T]](capacity))
          promise <- Stream.eval(Deferred[F, Unit])
          _ <- Stream.bracket {
            Async[F].fromFutureDelay { implicit ec =>
              val enqueue = enqueueFuture[F, Chunk[T]](queue, promise) _

              cursor
                .foldBulksM(()) { (_, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  if (chunk.isEmpty && cursor.tailable)
                    Future.successful(Cursor.Cont(()))
                  else
                    enqueue(chunk).flatMap {
                      case Left(_) =>
                        Future.successful(Cursor.Done())
                      case Right(_) =>
                        Future.successful(Cursor.Cont())
                    }
                }
              Future.successful(promise)
            }
          }(_.complete(()).void)
          stream <- StreamHelpers.fromQueueUnterminatedChunk(queue)
        } yield stream
      )
  }
}
