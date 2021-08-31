package com.bcf.reactivemongo4s.dsl.criteria

import reactivemongo.api.bson._

/**
  * The '''ValueBuilder'''ype is a model of the ''type class'' pattern used to
  * produce a ''T''-specific [[reactivemongo.api.bson.BSONValue]] instance.
  *
  * @author svickers
  */
trait ValueBuilder[T] {
  def bson(v: T): BSONValue
}

/**
  * The '''ValueBuilder''' companion object defines common
  * ValueBuilder ''type classes''
  * available for any project.  Types not known to the library can define
  * ValueBuilder instances as needed
  * to extend the DSL.
  */
object ValueBuilder {
  implicit def bsonValueIdentityValue[T <: BSONValue]: ValueBuilder[T] =
    (v: T) => v

  implicit object DateTimeValue extends ValueBuilder[java.util.Date] {
    override def bson(v: java.util.Date): BSONValue =
      BSONDateTime(v.getTime)
  }

  implicit object BooleanValue extends ValueBuilder[Boolean] {
    override def bson(v: Boolean): BSONValue =
      BSONBoolean(v)
  }

  implicit object DoubleValue extends ValueBuilder[Double] {
    override def bson(v: Double): BSONValue =
      BSONDouble(v)
  }

  implicit object IntValue extends ValueBuilder[Int] {
    override def bson(v: Int): BSONValue =
      BSONInteger(v)
  }

  implicit object LongValue extends ValueBuilder[Long] {
    override def bson(v: Long): BSONValue =
      BSONLong(v)
  }

  implicit object StringValue extends ValueBuilder[String] {
    override def bson(v: String): BSONValue =
      BSONString(v)
  }

  implicit object SymbolValue extends ValueBuilder[Symbol] {
    override def bson(v: Symbol): BSONValue =
      BSONSymbol(v.name)
  }

  implicit object TimestampValue extends ValueBuilder[java.sql.Timestamp] {
    override def bson(v: java.sql.Timestamp): BSONValue =
      BSONTimestamp(v.getTime)
  }
}
