package com.bcf.reactivemongo4s

import scala.collection.Factory
import scala.concurrent.Future

import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Deferred}
import cats.implicits._
import com.bcf.reactivemongo4s.helpers._
import fs2.{Chunk, Stream}
import reactivemongo.api.Cursor
import reactivemongo.api.Cursor.ErrorHandler

trait CursorOpsF {
  implicit final class CursorOpsFImpl[T](val cursor: Cursor[T]) {
    def headOptionF[F[_]: Async]: F[Option[T]] =
      Async[F].fromFutureDelay(cursor.headOption(_))

    def headF[F[_]: Async]: F[T] =
      Async[F].fromFutureDelay(cursor.head(_))

    def collectF[F[_]: Async, M[_]](maxDocs: Int = Int.MaxValue, err: ErrorHandler[M[T]] = Cursor.FailOnError[M[T]]())(implicit
        cbf: Factory[T, M[T]]
    ): F[M[T]] = Async[F].fromFutureDelay(cursor.collect(maxDocs, err)(cbf, _))

    def peekF[F[_]: Async, M[_]](maxDocs: Int)(implicit
        cbf: Factory[T, M[T]]
    ): F[Cursor.Result[M[T]]] = Async[F].fromFutureDelay(cursor.peek(maxDocs)(cbf, _))

    def toStream[F[_]: Async](
        queueCapacity: Int,
        errorHandler: CursorErrorHandler = CursorErrorHandler.Fail
    ): Stream[F, T] =
      for {
        dispatcher <- Stream.resource(Dispatcher[F])
        queue <- Stream.eval(Queue.bounded[F, Option[Either[Throwable, Chunk[T]]]](queueCapacity))
        promise <- Stream.eval(Deferred[F, Unit])
        _ <- Stream.bracket {
          Async[F].fromFutureDelay { implicit ec =>
            def enqueue(v: Option[Either[Throwable, Chunk[T]]]): Future[Either[Unit, Unit]] =
              dispatcher.unsafeToFuture(Async[F].race(promise.get, queue.offer(v)))

            cursor
              .foldBulksM(())(
                { (_, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  enqueue(chunk.asRight.some).flatMap {
                    case Left(_) =>
                      enqueue(None).map(_ => Cursor.Done())
                    case Right(_) =>
                      Future.successful(Cursor.Cont())
                  }
                },
                (_, err) =>
                  errorHandler match {
                    case CursorErrorHandler.Fail =>
                      enqueue(err.asLeft.some)
                      Cursor.Fail(err)
                    case CursorErrorHandler.Done =>
                      enqueue(None)
                      Cursor.Done()
                    case CursorErrorHandler.Cont =>
                      Cursor.Cont()
                  }
              )
              .flatMap(_ => enqueue(None))

            Future.successful(promise)
          }
        }(_.complete(()).void)
        stream <- Stream.fromQueueNoneTerminated(queue).rethrow.flatMap(Stream.chunk)
      } yield stream

    def toStreamUnterminated[F[_]: Async](capacity: Int): Stream[F, T] =
      for {
        dispatcher <- Stream.resource(Dispatcher[F])
        queue <- Stream.eval(Queue.bounded[F, Chunk[T]](capacity))
        promise <- Stream.eval(Deferred[F, Unit])
        _ <- Stream.bracket {
          Async[F].fromFutureDelay { implicit ec =>
            def enqueue(v: Chunk[T]): Future[Either[Unit, Unit]] =
              dispatcher.unsafeToFuture(Async[F].race(promise.get, queue.offer(v)))

            cursor
              .foldBulksM(()) { (_, xs) =>
                val chunk = Chunk.seq(xs.toSeq)
                if (
                  chunk.isEmpty
//                  && cursor.tailable TODO had to remove this to extend TestCursor in tests
                )
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
        stream <- Stream.fromQueueUnterminatedChunk(queue)
      } yield stream
  }
}
