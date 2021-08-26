package reactivemongo.api

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.core.protocol.Response

class FailAtCursor[T](elements: Seq[T], failAfter: Int, failWith: => Throwable, batchSize: Int = 10) extends TestCursor[T] with CursorOps[T] {
  override def foldBulksM[A](z: => A, maxDocs: Int)(suc: (A, Iterator[T]) => Future[Cursor.State[A]], err: Cursor.ErrorHandler[A])(implicit
      ctx: ExecutionContext
  ): Future[A] = {
    def go(z: => A, elements: Seq[T], count: Int = 0): Future[A] = {
      val (head, tail) = elements.splitAt(batchSize)

      val handler =
        if (failAfter == count) Future(err(z, failWith))
        else suc(z, head.iterator)

      handler.flatMap {
        case cont: Cursor.Cont[A] if tail.nonEmpty => go(cont.value, tail, count + 1)
        case cont: Cursor.Cont[A]                  => Future(cont.value)
        case done: Cursor.Done[A]                  => Future(done.value)
        case fail: Cursor.Fail[A]                  => Future.failed(fail.cause)
      }
    }

    go(z, elements)
  }

  private[reactivemongo] def makeRequest(maxDocs: Int)(implicit ec: ExecutionContext): Future[Response] = ???

  private[reactivemongo] def nextResponse(maxDocs: Int): (ExecutionContext, Response) => Future[Option[Response]] = ???

  private[reactivemongo] def documentIterator(response: Response): Iterator[T] = ???

  private[reactivemongo] def killCursor(id: Long)(implicit ec: ExecutionContext): Unit = ???

  def tailable: Boolean = false

  def connection: MongoConnection = ???

  def failoverStrategy: FailoverStrategy = ???

}
