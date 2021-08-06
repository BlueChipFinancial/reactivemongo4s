package com.bcf

import scala.concurrent.ExecutionContext

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
    MongoClientF[IO](Seq("localhost"), MongoConnectionOptions.default).use { con =>
      for {
        db <- con.getDatabase("test")
//        _ <- db.createCollection("test2")
        cols <- db.collectionNames
        _ <- IO.println(cols)
        col <- db.getCollection("test1")
        res <- col.find[MyModel](BSONDocument(
          "b" -> BSONDocument("$exists" -> true)
        ))
        _ <- res.evalMap(IO.println).compile.drain

//        _ <- res.evalMap(IO.println(_)).compile.drain
        count <- col.count()
        _ <- IO.println("Count: " + count)
      } yield ()
    }.as(ExitCode.Success)
}
