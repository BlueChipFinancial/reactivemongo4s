package com.bcf.reactivemongo4s.cursors

import scala.concurrent.{ExecutionContext, Future}

import reactivemongo.api.Cursor.ErrorHandler
import reactivemongo.api.{Cursor, TestCursor}

class NormalCursor[T](elements: Seq[T], batchSize: Int = 10) extends TestCursor[T] {
  override def headOption(implicit ctx: ExecutionContext): Future[Option[T]] = Future(elements.headOption)

  override def foldBulksM[A](z: => A, maxDocs: Int)(suc: (A, Iterator[T]) => Future[Cursor.State[A]], err: ErrorHandler[A])(implicit
      ctx: ExecutionContext
  ): Future[A] = {
    def go(z: => A, elements: Seq[T]): Future[A] = {
      val (head, tail) = elements.splitAt(batchSize)
      suc(z, head.iterator).flatMap {
        case cont: Cursor.Cont[A] if tail.nonEmpty => go(cont.value, tail)
        case cont: Cursor.Cont[A]                  => Future(cont.value)
        case done: Cursor.Done[A]                  => Future(done.value)
        case fail: Cursor.Fail[A]                  => Future.failed(fail.cause)
      }
    }

    go(z, elements)
  }
}
