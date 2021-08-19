package com.bcf.reactivemongo4s

import cats.Functor
import cats.implicits._
import fs2.concurrent.Queue
import fs2.{Chunk, Stream}

object StreamHelpers {
  def fromQueueUnterminatedChunk[F[_]: Functor, A](
      queue: Queue[F, Chunk[A]],
      limit: Int = Int.MaxValue
  ): Stream[F, A] =
    fromQueueNoneTerminatedChunk_[F, A](
      queue.dequeue1.map(Some(_)),
      queue.tryDequeue1.map(_.map(Some(_))),
      limit
    )

  def fromQueueNoneTerminatedChunk[F[_], A](
      queue: Queue[F, Option[Chunk[A]]],
      limit: Int = Int.MaxValue
  ): Stream[F, A] =
    fromQueueNoneTerminatedChunk_(queue.dequeue1, queue.tryDequeue1, limit)

  private def fromQueueNoneTerminatedChunk_[F[_], A](
      take: F[Option[Chunk[A]]],
      tryTake: F[Option[Option[Chunk[A]]]],
      limit: Int
  ): Stream[F, A] = {
    def await: Stream[F, A] =
      Stream.eval(take).flatMap {
        case None    => Stream.empty
        case Some(c) => pump(c)
      }
    def pump(acc: Chunk[A]): Stream[F, A] = {
      val sz = acc.size
      if (sz > limit) {
        val (pfx, sfx) = acc.splitAt(limit)
        Stream.chunk(pfx) ++ pump(sfx)
      } else if (sz == limit) Stream.chunk(acc) ++ await
      else
        Stream.eval(tryTake).flatMap {
          case None          => Stream.chunk(acc) ++ await
          case Some(Some(c)) => pump(Chunk.concat(Seq(acc, c)))
          case Some(None)    => Stream.chunk(acc)
        }
    }
    await
  }
}
