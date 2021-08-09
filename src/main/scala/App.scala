package com.bcf

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.{ExitCode, IO, IOApp}
import reactivemongo.api.MongoConnectionOptions
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader}

object App extends IOApp {

  implicit val ec = ExecutionContext.global

  case class MyModel(age: Int)

  implicit val reader: BSONDocumentReader[MyModel] = BSONDocumentReader.from { bson =>
    for {
      age <- bson.getAsTry[Int]("b")
    } yield MyModel(age)
  }

  override def run(args: List[String]): IO[ExitCode] =
    MongoClientF[IO](Seq("localhost"), MongoConnectionOptions.default)
      .use { con =>
        for {
          db <- con.getDatabase("test")
          cols <- db.collectionNames
          _ <- IO.println(cols)
          col <- db.getCollection("test2")
          res <- col.find[MyModel](BSONDocument("b" -> BSONDocument("$exists" -> true)))
          _ <- res.evalMap(model => IO.println(s"Got $model")).compile.drain
          _ <- IO.println("Finished streaming models")
          count <- col.count()
          _ <- IO.println("Count: " + count)
        } yield ()
      }
      .timeout(5.seconds)
      .as(ExitCode.Success)
}
