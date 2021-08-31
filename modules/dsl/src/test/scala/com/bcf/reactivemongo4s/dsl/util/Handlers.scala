package com.bcf.reactivemongo4s.dsl.util

import scala.util.Try

import cats.implicits._
import org.joda.time.DateTime
import reactivemongo.api.bson._

object Handlers {
  implicit def BSONDateTimeWriter: BSONWriter[DateTime] =
    BSONWriter { dateTime =>
      BSONDateTime(dateTime.getMillis)
    }

  implicit def BSONDateTimeReader: BSONReader[DateTime] =
    BSONReader { bson =>
      new DateTime(bson.asInstanceOf[BSONDateTime].value)
    }

  implicit def MapBSONReader[T](implicit reader: BSONReader[T]): BSONDocumentReader[Map[String, T]] =
    (doc: BSONDocument) => {
      doc.elements
        .collect {
          case BSONElement(key, value) =>
            value.asTry[T](reader) map { ov =>
              (key, ov)
            }
        }
        .sequence
        .map(_.toMap)
    }

  private def mapToDoc[V](t: Map[String, V])(implicit writer: BSONWriter[V]) =
    t.toSeq.map { case (k, v) => writer.writeTry(v).map((k, _)) }.sequence.map(BSONDocument.apply)

  implicit def MapBSONWriter[T](implicit writer: BSONWriter[T]): BSONDocumentWriter[Map[String, T]] =
    (t: Map[String, T]) => mapToDoc(t)

  implicit def MapReader[V](implicit vr: BSONDocumentReader[V]): BSONDocumentReader[Map[String, V]] =
    (doc: BSONDocument) =>
      doc.elements
        .map { el =>
          vr.readTry(el.value).map(el.name -> _)
        }
        .sequence
        .map(_.toMap)

  implicit def MapWriter[V](implicit vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[String, V]] =
    (t: Map[String, V]) => mapToDoc(t)

  implicit object BSONIntegerHandler extends BSONReader[Int] {
    def readTry(bson: BSONValue): Try[Int] = bson.asTry[BSONNumberLike].flatMap(_.toInt)
  }

  implicit object BSONLongHandler extends BSONReader[Long] {
    def readTry(bson: BSONValue): Try[Long] = bson.asTry[BSONNumberLike].flatMap(_.toLong)
  }

  implicit object BSONDoubleHandler extends BSONReader[Double] {
    def readTry(bson: BSONValue): Try[Double] = bson.asTry[BSONNumberLike].flatMap(_.toDouble)
  }

}
