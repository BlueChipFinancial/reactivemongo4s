package com.bcf.reactivemongo4s.dsl

import scala.reflect.runtime.universe.TypeTag
import scala.util.{Failure, Success}

import reactivemongo.api.bson._

trait BsonDsl {

  implicit def nameValueToProducer[T](pair: (String, T))(implicit writer: BSONWriter[T]): ElementProducer =
    writer.writeTry(pair._2) match {
      case Success(v) => BSONElement(pair._1, v)
      case Failure(e) => throw e // scalafix:ok
    }

  implicit def exprToProducer[V <: BSONValue, A <: Expression[V]](expression: A): ElementProducer =
    expression.field -> expression.value

  implicit def exprToBSONDocument[V <: BSONValue](expression: Expression[V])(implicit writer: BSONWriter[V]): BSONDocument =
    BSONDocument(expression.field -> expression.value)

  implicit def bsonDocumentToPretty(document: BSONDocument): String =
    BSONDocument.pretty(document)

  //**********************************************************************************************//
  // Helpers
  def $empty: BSONDocument = BSONDocument.empty

  def $doc(elements: ElementProducer*): BSONDocument =
    BSONDocument(elements: _*)

  def $arr(elements: Producer[BSONValue]*): BSONArray =
    BSONArray(elements: _*)

  def $id[T: BSONWriter](id: T): BSONDocument = BSONDocument("_id" -> id)

  // End of Helpers
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Logical Operators
  def $or(expressions: BSONDocument*): BSONDocument =
    BSONDocument("$or" -> expressions)

  def $and(expressions: BSONDocument*): BSONDocument =
    BSONDocument("$and" -> expressions)

  def $nor(expressions: BSONDocument*): BSONDocument =
    BSONDocument("$nor" -> expressions)
  // End of Top Level Logical Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Evaluation Operators
  def $text(search: String): BSONDocument =
    BSONDocument("$text" -> BSONDocument("$search" -> search))

  def $text(search: String, language: String): BSONDocument =
    BSONDocument("$text" -> BSONDocument("$search" -> search, "$language" -> language))

  def $where(expression: String): BSONDocument =
    BSONDocument("$where" -> expression)
  // End of Top Level Evaluation Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Field Update Operators
  def $inc(item: ElementProducer, items: ElementProducer*): BSONDocument =
    BSONDocument("$inc" -> BSONDocument(Seq(item) ++ items: _*))

  def $mul(item: ElementProducer): BSONDocument =
    BSONDocument("$mul" -> BSONDocument(item))

  def $rename(item: (String, String), items: (String, String)*): BSONDocument =
    BSONDocument("$rename" -> BSONDocument((Seq(item) ++ items).map(nameValueToProducer[String]): _*))

  def $setOnInsert(item: ElementProducer, items: ElementProducer*): BSONDocument =
    BSONDocument("$setOnInsert" -> BSONDocument(Seq(item) ++ items: _*))

  def $set(item: ElementProducer, items: ElementProducer*): BSONDocument =
    BSONDocument("$set" -> BSONDocument(Seq(item) ++ items: _*))

  def $unset(field: String, fields: String*): BSONDocument =
    BSONDocument("$unset" -> BSONDocument((Seq(field) ++ fields).map(_ -> BSONString(""))))

  def $min(item: ElementProducer): BSONDocument =
    BSONDocument("$min" -> BSONDocument(item))

  def $max(item: ElementProducer): BSONDocument =
    BSONDocument("$max" -> BSONDocument(item))

  trait CurrentDateValueProducer[T] {
    def produce: BSONValue
  }

  implicit class BooleanCurrentDateValueProducer(value: Boolean) extends CurrentDateValueProducer[Boolean] {
    def produce: BSONValue = BSONBoolean(value)
  }

  implicit class StringCurrentDateValueProducer(value: String) extends CurrentDateValueProducer[String] {
    def isValid: Boolean = Seq("date", "timestamp") contains value

    def produce: BSONValue = {
      if (!isValid)
        throw new IllegalArgumentException(value) // scalafix:ok

      BSONDocument("$type" -> value)
    }
  }

  def $currentDate(items: (String, CurrentDateValueProducer[_])*): BSONDocument =
    BSONDocument("$currentDate" -> BSONDocument(items.map(item => item._1 -> item._2.produce)))
  // End of Top Level Field Update Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Array Update Operators
  def $addToSet(item: ElementProducer, items: ElementProducer*): BSONDocument =
    BSONDocument("$addToSet" -> BSONDocument(Seq(item) ++ items: _*))

  def $pop(item: (String, Int)): BSONDocument = {
    if (item._2 != -1 && item._2 != 1)
      throw new IllegalArgumentException(s"${item._2} is not equal to: -1 | 1") // scalafix:ok

    BSONDocument("$pop" -> BSONDocument(item))
  }

  def $push(item: ElementProducer): BSONDocument =
    BSONDocument("$push" -> BSONDocument(item))

  def $pushEach[T](field: String, values: T*)(implicit writer: BSONWriter[T]): BSONDocument =
    BSONDocument(
      "$push" -> BSONDocument(
        field -> BSONDocument(
          "$each" -> values
        )
      )
    )

  def $pull(item: ElementProducer): BSONDocument =
    BSONDocument("$pull" -> BSONDocument(item))
  // End ofTop Level Array Update Operators
  //**********************************************************************************************//

  /**
    * Represents the inital state of the expression which has only the name of the field.
    * It does not know the value of the expression.
    */
  trait ElementBuilder {
    def field: String
    def append(value: BSONDocument): BSONDocument = value
  }

  /** Represents the state of an expression which has a field and a value */
  trait Expression[V <: BSONValue] extends ElementBuilder {
    def value: V
  }

  /*
   * This type of expressions cannot be cascaded. Examples:
   *
   * {{{
   * "price" $eq 10
   * "price" $ne 1000
   * "size" $in ("S", "M", "L")
   * "size" $nin ("S", "XXL")
   * }}}
   *
   */
  case class SimpleExpression[V <: BSONValue](field: String, value: V) extends Expression[V]

  /**
    * Expressions of this type can be cascaded. Examples:
    *
    * {{{
    *  "age" $gt 50 $lt 60
    *  "age" $gte 50 $lte 60
    * }}}
    */
  case class CompositeExpression(field: String, value: BSONDocument) extends Expression[BSONDocument] with ComparisonOperators {
    override def append(value: BSONDocument): BSONDocument =
      this.value ++ value
  }

  /** MongoDB comparison operators. */
  trait ComparisonOperators { self: ElementBuilder =>

    def $eq[T](value: T)(implicit writer: BSONWriter[T]): SimpleExpression[BSONValue] =
      SimpleExpression(field, writer.writeOpt(value).get)

    /** Matches values that are greater than the value specified in the query. */
    def $gt[T](value: T)(implicit writer: BSONWriter[T]): CompositeExpression =
      CompositeExpression(field, append(BSONDocument("$gt" -> value)))

    /** Matches values that are greater than or equal to the value specified in the query. */
    def $gte[T](value: T)(implicit writer: BSONWriter[T]): CompositeExpression =
      CompositeExpression(field, append(BSONDocument("$gte" -> value)))

    /** Matches any of the values that exist in an array specified in the query. */
    def $in[T](values: T*)(implicit writer: BSONWriter[T]): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$in" -> values))

    /** Matches values that are less than the value specified in the query. */
    def $lt[T](value: T)(implicit writer: BSONWriter[T]): CompositeExpression =
      CompositeExpression(field, append(BSONDocument("$lt" -> value)))

    /** Matches values that are less than or equal to the value specified in the query. */
    def $lte[T](value: T)(implicit writer: BSONWriter[T]): CompositeExpression =
      CompositeExpression(field, append(BSONDocument("$lte" -> value)))

    /** Matches all values that are not equal to the value specified in the query. */
    def $ne[T](value: T)(implicit writer: BSONWriter[T]): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$ne" -> value))

    /** Matches values that do not exist in an array specified to the query. */
    def $nin[T](values: T*)(implicit writer: BSONWriter[T]): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$nin" -> values))

  }

  trait LogicalOperators { self: ElementBuilder =>
    def $not(f: String => Expression[BSONDocument]): SimpleExpression[BSONDocument] = {
      val expression = f(field)
      SimpleExpression(field, BSONDocument("$not" -> expression.value))
    }
  }

  trait ElementOperators { self: ElementBuilder =>
    def $exists(exists: Boolean): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$exists" -> exists))

    def $type[T <: BSONValue: TypeTag]: SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$type" -> BsonTypes.numberOf[T]))
  }

  trait EvaluationOperators { self: ElementBuilder =>
    def $mod(divisor: Int, remainder: Int): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$mod" -> BSONArray(divisor, remainder)))

    def $regex(value: String, options: String): SimpleExpression[BSONRegex] =
      SimpleExpression(field, BSONRegex(value, options))
  }

  trait ArrayOperators { self: ElementBuilder =>
    def $all[T](values: T*)(implicit writer: BSONWriter[T]): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$all" -> values))

    def $elemMatch(query: ElementProducer*): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$elemMatch" -> BSONDocument(query: _*)))

    def $size(size: Int): SimpleExpression[BSONDocument] =
      SimpleExpression(field, BSONDocument("$size" -> size))
  }

  implicit class ElementBuilderLike(val field: String)
      extends ElementBuilder
      with ComparisonOperators
      with ElementOperators
      with EvaluationOperators
      with LogicalOperators
      with ArrayOperators
}

object BsonDsl extends BsonDsl
