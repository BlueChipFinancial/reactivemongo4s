package com.bcf

import scala.concurrent.ExecutionContext

import cats.effect.{ExitCode, IO, IOApp}
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader}
import reactivemongo.api.{MongoConnectionOptions, ReadConcern}
import helpers._
import cats.implicits._

object App extends IOApp {

  implicit val ec: ExecutionContext = ExecutionContext.global

  case class MyModel(age: Int)

  implicit val reader: BSONDocumentReader[MyModel] = BSONDocumentReader.from { bson =>
    for {
      age <- bson.getAsTry[Int]("b")
    } yield MyModel(age)
  }

  override def run(args: List[String]): IO[ExitCode] =
    MongoClientF[IO](Seq("localhost"), MongoConnectionOptions.default.copy(keepAlive = true, readConcern = ReadConcern.Majority))
      .use { con =>
        for {
          db <- con.getDatabase("test")
          cols <- db.collectionNames
          _ <- IO.println(cols)
          col <- db.getCollection("test1")
          res <- col.find[MyModel](BSONDocument("b" -> BSONDocument("$exists" -> true)))
          _ <- res.evalMap(model => IO.println(s"Got $model")).compile.drain
          _ <- IO.println("Finished streaming models")
          count <- col.count()
          _ <- IO.println("Count: " + count)
        } yield ()
      }
      .as(ExitCode.Success)
}
