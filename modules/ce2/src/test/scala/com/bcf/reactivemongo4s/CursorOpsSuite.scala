package com.bcf.reactivemongo4s

import java.util.NoSuchElementException

import scala.concurrent.ExecutionContext

import cats.effect.IO
import com.bcf.reactivemongo4s.cursors.{FailAtCursor, NormalCursor}
import com.bcf.reactivemongo4s.implicits._
import weaver._

object CursorOpsSuite extends SimpleIOSuite {
  implicit val me: MongoExecutor[IO] = MongoExecutor.fromExecutionContext[IO](ExecutionContext.global)

  test("Consume all documents from normal cursor") {
    val elements = 1 to 1000
    val cursor = new NormalCursor[Int](elements)
    for {
      a <- cursor.toStream[IO](10).compile.toList
      b <- IO(elements.toList)
    } yield expect(a == b)
  }

  test("Done after cursor fail") {
    val elements = 1 to 1000
    val cursor = new FailAtCursor[Int](elements, 2, new NoSuchElementException, 10)
    for {
      a <- cursor.toStream[IO](10, CursorErrorHandler.Done).compile.toList
      b <- IO((1 to 20).toList)
    } yield expect(a == b)
  }

  test("Continue after cursor fail") {
    val elements = 1 to 1000
    val cursor = new FailAtCursor[Int](elements, 2, new NoSuchElementException, 10)
    for {
      a <- cursor.toStream[IO](10, CursorErrorHandler.Cont).compile.toList
      b <- IO(((1 to 20) ++ (31 to 1000)).toList)
    } yield expect(a == b)
  }

  test("Fail stream with error") {
    val elements = 1 to 1000
    val cursor = new FailAtCursor[Int](elements, 2, new NoSuchElementException, 10)
    for {
      a <-
        cursor
          .toStream[IO](10, CursorErrorHandler.Fail)
          .handleErrorWith {
            case _: NoSuchElementException => fs2.Stream(-1)
          }
          .compile
          .toList
    } yield expect(a.contains(-1))
  }
}
