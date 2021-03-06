package com.bcf.reactivemongo4s.dsl.criteria

import scala.language.dynamics

import reactivemongo.api.bson._

/**
  * A '''Term''' instance reifies the use of a MongoDB document field, both
  * top-level or nested.  Operators common to all ''T'' types are defined here
  * with type-specific ones provided in the companion object below.
  *
  * @author svickers
  */
case class Term[T](`_term$name`: String) extends Dynamic {

  /**
    * Logical equality.
    */
  def ===[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      `_term$name` -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Logical equality.
    */
  def @==[U <: T: ValueBuilder](rhs: U): Expression =
    ===[U](rhs)

  /**
    * Logical inequality: '''$ne'''.
    */
  def <>[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      "$ne" -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Logical inequality: '''$ne'''.
    */
  def =/=[U <: T: ValueBuilder](rhs: U): Expression =
    <>[U](rhs)

  /**
    * Logical inequality: '''$ne'''.
    */
  def !==[U <: T: ValueBuilder](rhs: U): Expression =
    <>[U](rhs)

  /**
    * Less-than comparison: '''$lt'''.
    */
  def <[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      "$lt" -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Less-than or equal comparison: '''$lte'''.
    */
  def <=[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      "$lte" -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Greater-than comparison: '''$gt'''.
    */
  def >[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      "$gt" -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Greater-than or equal comparison: '''$gte'''.
    */
  def >=[U <: T: ValueBuilder](rhs: U): Expression =
    Expression(
      `_term$name`,
      "$gte" -> implicitly[ValueBuilder[U]].bson(rhs)
    )

  /**
    * Field existence: '''$exists'''.
    */
  def exists: Expression =
    Expression(`_term$name`, "$exists" -> BSONBoolean(true))

  /**
    * Field value equals one of the '''values''': '''$in'''.
    */
  def in[U <: T: ValueBuilder](values: Iterable[U])(implicit B: ValueBuilder[U]): Expression =
    Expression(`_term$name`, "$in" -> BSONArray(values map B.bson))

  /**
    * Field value equals either '''head''' or one of the (optional) '''tail''' values: '''$in'''.
    */
  def in[U <: T: ValueBuilder](head: U, tail: U*)(implicit B: ValueBuilder[U]): Expression =
    Expression(
      `_term$name`,
      "$in" -> BSONArray(Seq(B.bson(head)) ++ tail.map(B.bson))
    )

  def selectDynamic[U](field: String): Term[U] =
    Term[U](`_term$name` + "." + field)
}

object Term {
  /// Class Types
  /**
    * The '''CollectionTermOps''' `implicit` provides EDSL functionality to
    * `Seq` Terms only.
    */
  implicit class CollectionTermOps[T](val term: Term[Seq[T]]) extends AnyVal {
    def all(values: Iterable[T])(implicit B: ValueBuilder[T]): Expression =
      Expression(
        term.`_term$name`,
        "$all" -> BSONArray(values map B.bson)
      )
  }

  /**
    * The '''StringTermOps''' `implicit` enriches
    * Terms for `String`-only operations.
    */
  implicit class StringTermOps[T >: String](val term: Term[T]) extends AnyVal {
    def =~(re: String): Expression =
      Expression(term.`_term$name`, "$regex" -> BSONRegex(re, ""))

    def !~(re: String): Expression =
      Expression(
        term.`_term$name`,
        "$not" -> BSONDocument("$regex" -> BSONRegex(re, ""))
      )
  }
}
