package com.bcf

import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Deferred, Sync}
import cats._
import cats.data._
import cats.implicits._
import reactivemongo.api.Cursor
import fs2.{Chunk, Stream}
import reactivemongo.api.Cursor.WithOps

object helpers {

  implicit final class AsyncExtended[F[_]](private val F: Async[F]) extends AnyVal {
    def fromFutureDelay[A](thunk: => Future[A]): F[A] = F.fromFuture(F.delay(thunk))
  }

  implicit final class CursorExtended[F[_]: Async, T](private val cursor: Cursor[T] with WithOps[T]) {
    def headOptionF(implicit ec: ExecutionContext): F[Option[T]] = Async[F].fromFutureDelay(cursor.headOption)

    def toStream(implicit ec: ExecutionContext): F[Stream[F, T]] = {
      val stream = Stream.unfoldEval(cursor)(e => OptionT(e.headOptionF).map((_, e)).value)
      Sync[F].delay(stream)
    }

    def toStream2(implicit ec: ExecutionContext): F[Stream[F, T]] =
      Async[F] fromFutureDelay cursor.foldWhile(Stream.empty: Stream[F, T])(
        (ls, str) => // process next String value
          Cursor.Cont(Stream.emit(str) ++ ls) // Continue with updated `ls`
        ,
        (ls, err) => // handle failure
          err match {
            case _: RuntimeException => Cursor.Cont(ls) // Skip error, continue
            case _                   => Cursor.Fail(err) // Stop with current failure -> Future.failed
          }
      )

    def toStream3(capacity: Int)(implicit ec: ExecutionContext): F[Stream[F, T]] = {
      val s = for {
        dispatcher <- Stream.resource(Dispatcher[F])
        q <- Stream.eval(Queue.unbounded[F, Option[Chunk[T]]])
        d <- Stream.eval(Deferred[F, Unit])
        _ <- Stream.eval {
          Async[F].fromFutureDelay {
            def enqueue(v: Option[Chunk[T]]): Future[Either[Unit, Unit]] = {
              println(s"Equeing chunks: $v")
              dispatcher.unsafeToFuture(Async[F].race(d.get, q.offer(v)))
            }

            cursor.foldBulksM(()) { (x, xs) =>
              val chunk = Chunk.seq(xs.toSeq)
              enqueue(chunk.some).flatMap {
                case Left(_) =>
                  enqueue(None).map { _ =>
                    println("Signalling I'm done!!!")
                    Cursor.Done(())
                  }
                case Right(_) => Future.successful(Cursor.Cont())
              }
            }
          }
        }
        stream <- Stream.fromQueueNoneTerminatedChunk(q).onFinalize(d.complete(()).void)
      } yield stream

      Sync[F].delay(s)
    }

    def toStream4(capacity: Int)(implicit ec: ExecutionContext): F[Stream[F, T]] =
      Sync[F].delay(
        for {
          dispatcher <- Stream.resource(Dispatcher[F])
          queue <- Stream.eval(Queue.bounded[F, Option[Chunk[T]]](capacity))
          promise <- Stream.eval(Deferred[F, Unit])
          _ <- Stream.eval(Sync[F].delay(println(s"Is tailable: ${cursor.tailable}")))
          _ <- Stream.eval {
            Async[F].fromFutureDelay {
              def enqueue(v: Option[Chunk[T]]): Future[Either[Unit, Unit]] = {
                println(s"Equeing chunks: $v")
                dispatcher.unsafeToFuture(Async[F].race(promise.get, queue.offer(v)))
              }

              cursor
                .foldBulksM(()) { (x, xs) =>
                  val chunk = Chunk.seq(xs.toSeq)
                  enqueue(chunk.some).flatMap {
                    case Left(_) =>
                      println("Signalling I'm done!!!")
                      enqueue(None).map { _ =>
                        println("Returning done!!!")
                        Cursor.Done()
                      }
                    case Right(_) =>
                      println("Well, I'm right")
                      Future.successful(Cursor.Cont())
                  }
                }
                .flatMap { _ =>
                  println("Completing the promise!")
                  enqueue(None)
                //dispatcher.unsafeToFuture(promise.complete(()))
                }
            }
          }
          stream <- Stream.fromQueueNoneTerminatedChunk(queue).onFinalize(promise.complete(()).void)
        } yield stream
      )
  }

}
