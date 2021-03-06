package com.bcf.reactivemongo4s.dsl.criteria

import scala.language.implicitConversions
import scala.util.Try

import reactivemongo.api.bson.BSONElement._
import reactivemongo.api.bson._

/**
  * The '''Expression''' type defines a recursive propositional abstract
  * syntax tree central to the MongoDB embedded domain-specific language (EDSL).
  * It is the main abstraction used to provide the EDSL and results in being
  * able to write:
  *
  * {{{
  * import Untyped._
  *
  * val edslQuery = criteria.first < 10 && (
  *  criteria.second >= 20.0 || criteria.second.in (0.0, 1.0)
  *  );
  * }}}
  *
  * And have that equivalent to this filter:
  *
  * {{{
  * val bsonQuery = BSONDocument (
  *   "$and" ->
  *   BSONArray (
  *    BSONDocument (
  *     "first" -> BSONDocument ("$lt" -> BSONInteger (10))
  *     ),
  *   BSONDocument (
  *    "$or" ->
  *    BSONArray (
  *     BSONDocument (
  *      "second" -> BSONDocument ("$gte" -> BSONDouble (20.0))
  *      ),
  *     BSONDocument (
  *      "second" ->
  *      BSONDocument (
  *       "$in" -> BSONArray (BSONDouble (0.0), BSONDouble (1.0))
  *       )
  *      )
  *     )
  *    )
  *   )
  *  );
  * }}}
  *
  * @author svickers
  */
case class Expression(name: Option[String], element: BSONElement) {
  /// Class Imports
  import Expression._

  /**
    * The logical negation operator attempts to invert this '''Expression'''
    * by using complimentary operators if possible, falling back to the
    * general-case wrapping in a `$not` operator.
    */
  def unary_! : Expression =
    this match {
      case Expression(Some(term), BSONElement("$in", vals)) =>
        Expression(term, ("$nin", vals));

      case Expression(Some(term), BSONElement("$nin", vals)) =>
        Expression(term, ("$in", vals));

      case Expression(Some(term), BSONElement("$ne", vals)) =>
        Expression(term, (term, vals));

      case Expression(Some(term), BSONElement(field, vals)) if field == term =>
        Expression(term, ("$ne", vals));

      case Expression(None, BSONElement("$nor", vals)) =>
        Expression(None, "$or" -> vals);

      case Expression(None, BSONElement("$or", vals)) =>
        Expression(None, "$nor" -> vals);

      case Expression(Some("$not"), el) =>
        Expression(None, el);

      case Expression(Some(n), _) =>
        Expression(Some("$not"), n -> BSONDocument(element));

      case Expression(None, el) =>
        Expression(Some("$not"), el);
    }

  /**
    * Conjunction: ''AND''.
    */
  def &&(rhs: Expression): Expression = combine("$and", rhs)

  /**
    * Negation of conjunction: ''NOR''.
    */
  def !&&(rhs: Expression): Expression = combine("$nor", rhs)

  /**
    * Disjunction: ''OR''.
    */
  def ||(rhs: Expression): Expression = combine("$or", rhs)

  /**
    * The isEmpty method reports as to whether or not this '''Expression'''
    * has neither a `name` nor an assigned value.
    */
  def isEmpty: Boolean = name.isEmpty && element.name.isEmpty

  private def combine(op: String, rhs: Expression): Expression =
    if (rhs.isEmpty)
      this
    else
      element match {
        case BSONElement(`op`, arr: BSONArray) =>
          Expression(
            None,
            (op, arr ++ BSONArray(toBSONDocument(rhs)))
          );

        case BSONElement("", _) =>
          rhs;

        case _ =>
          Expression(
            None,
            op -> BSONArray(toBSONDocument(this), toBSONDocument(rhs))
          );
      }
}

object Expression {

  /**
    * The empty property is provided so that ''monoid'' definitions for
    * '''Expression''' can be easily provided.
    */
  val empty = new Expression(None, "" -> BSONDocument.empty)

  def apply(name: String, element: BSONElement): Expression =
    new Expression(Some(name), element)

  /// Implicit Conversions
  implicit object ExpressionWriter extends BSONDocumentWriter[Expression] {
    override def writeTry(t: Expression): Try[BSONDocument] = Try(toBSONDocument(t))
  }

  implicit def toBSONDocument(expr: Expression): BSONDocument =
    expr match {
      case Expression(Some(name), BSONElement(field, element)) if name == field =>
        BSONDocument(field -> element);

      case Expression(Some(name), element) =>
        BSONDocument(name -> BSONDocument(element));

      case Expression(None, BSONElement("", _)) =>
        BSONDocument.empty;

      case Expression(None, element) =>
        BSONDocument(element);
    }

  implicit def toBSONElement(expr: Expression): BSONElement =
    expr.element
}
