package com.bcf.reactivemongo4s.dsl.criteria

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reactivemongo.api.bson._

/**
  * The '''UntypedCriteriaSpec''' type unit tests the
  * [[com.bcf.reactivemongo4s.dsl.criteria.Untyped]] EDSL functionality and
  * serves both to verify fitness as well as an exemplar to how the
  * [[com.bcf.reactivemongo4s.dsl.criteria.Untyped]] functionality can be used.
  *
  * @author svickers
  */
class UntypedCriteriaSpec extends AnyFlatSpec with Matchers {
  /// Class Imports
  import Untyped._

  "An Untyped criteria" should "support equality comparisons" in {
    BSONDocument.pretty(criteria.myField === "a value") shouldBe BSONDocument.pretty {
      BSONDocument(
        "myField" -> BSONString("a value")
      )
    }

    BSONDocument.pretty(criteria.myField @== "a value") shouldBe BSONDocument.pretty(
      BSONDocument(
        "myField" -> BSONString("a value")
      )
    )
  }

  it should "support inequality comparisons" in {
    BSONDocument.pretty(criteria.myField !== "a value") shouldBe BSONDocument.pretty(
      BSONDocument(
        "myField" ->
          BSONDocument("$ne" -> BSONString("a value"))
      )
    )

    BSONDocument.pretty(criteria.myField =/= "a value") shouldBe BSONDocument.pretty(
      BSONDocument(
        "myField" ->
          BSONDocument("$ne" -> BSONString("a value"))
      )
    )

    BSONDocument.pretty(criteria.myField <> "a value") shouldBe BSONDocument.pretty(
      BSONDocument(
        "myField" ->
          BSONDocument("$ne" -> BSONString("a value"))
      )
    )
  }

  it should "support nested object selectors" in {
    val q = criteria.outer.inner =/= 99

    BSONDocument.pretty(q) shouldBe BSONDocument.pretty(
      BSONDocument(
        "outer.inner" ->
          BSONDocument("$ne" -> BSONInteger(99))
      )
    )
  }

  it should "support String operations" in {
    val q = criteria.str =~ "^test|re"

    BSONDocument.pretty(q) shouldBe BSONDocument.pretty(
      BSONDocument(
        "str" ->
          BSONDocument(
            "$regex" -> BSONRegex("^test|re", "")
          )
      )
    )
  }

  it should "support conjunctions" in {
    val q = criteria.first < 10 && criteria.second >= 20.0

    BSONDocument.pretty(BSONDocument(q.element)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$and" ->
          BSONArray(
            BSONDocument(
              "first" -> BSONDocument("$lt" -> BSONInteger(10))
            ),
            BSONDocument(
              "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
            )
          )
      )
    )
  }

  it should "support disjunctions" in {
    val q = criteria.first < 10 || criteria.second >= 20.0

    BSONDocument.pretty(BSONDocument(q.element)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$or" ->
          BSONArray(
            BSONDocument(
              "first" -> BSONDocument("$lt" -> BSONInteger(10))
            ),
            BSONDocument(
              "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
            )
          )
      )
    )
  }

  it should "combine adjacent conjunctions" in {
    val q = criteria.first < 10 && criteria.second >= 20.0 && criteria.third < 0.0

    BSONDocument.pretty(BSONDocument(q.element)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$and" ->
          BSONArray(
            BSONDocument(
              "first" -> BSONDocument("$lt" -> BSONInteger(10))
            ),
            BSONDocument(
              "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
            ),
            BSONDocument(
              "third" -> BSONDocument("$lt" -> BSONDouble(0.0))
            )
          )
      )
    )
  }

  it should "combine adjacent disjunctions" in {
    val q = criteria.first < 10 || criteria.second >= 20.0 || criteria.third < 0.0

    BSONDocument.pretty(BSONDocument(q.element)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$or" ->
          BSONArray(
            BSONDocument(
              "first" -> BSONDocument("$lt" -> BSONInteger(10))
            ),
            BSONDocument(
              "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
            ),
            BSONDocument(
              "third" -> BSONDocument("$lt" -> BSONDouble(0.0))
            )
          )
      )
    )
  }

  it should "support compound filtering" in {
    val q = criteria.first < 10 && (criteria.second >= 20.0 || criteria.second.in(0.0, 1.0))

    BSONDocument.pretty(q) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$and" ->
          BSONArray(
            BSONDocument(
              "first" -> BSONDocument("$lt" -> BSONInteger(10))
            ),
            BSONDocument(
              "$or" ->
                BSONArray(
                  BSONDocument(
                    "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
                  ),
                  BSONDocument(
                    "second" ->
                      BSONDocument(
                        "$in" ->
                          BSONArray(
                            BSONDouble(0.0),
                            BSONDouble(1.0)
                          )
                      )
                  )
                )
            )
          )
      )
    )
  }

  it should "support alternating logical operators" in {
    val q = criteria.first < 10 && criteria.second >= 20.0 || criteria.third < 0.0 && criteria.fourth =~ "some regex"

    BSONDocument.pretty(BSONDocument(q.element)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$or" ->
          BSONArray(
            BSONDocument(
              "$and" ->
                BSONArray(
                  BSONDocument(
                    "first" -> BSONDocument("$lt" -> BSONInteger(10))
                  ),
                  BSONDocument(
                    "second" -> BSONDocument("$gte" -> BSONDouble(20.0))
                  )
                )
            ),
            BSONDocument(
              "$and" ->
                BSONArray(
                  BSONDocument(
                    "third" -> BSONDocument("$lt" -> BSONDouble(0.0))
                  ),
                  BSONDocument(
                    "fourth" -> BSONDocument("$regex" -> BSONRegex("some regex", ""))
                  )
                )
            )
          )
      )
    )
  }

  it should "support logical negation" in {
    BSONDocument.pretty(!(criteria.a === 42)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "a" ->
          BSONDocument("$ne" -> BSONInteger(42))
      )
    )

    BSONDocument.pretty(!(criteria.a =~ "regex(p)?")) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$not" ->
          BSONDocument(
            "a" ->
              BSONDocument("$regex" -> BSONRegex("regex(p)?", ""))
          )
      )
    )

    BSONDocument.pretty(!(criteria.xyz === 1 || criteria.xyz === 2)) shouldBe BSONDocument.pretty(
      BSONDocument(
        "$nor" ->
          BSONArray(
            BSONDocument("xyz" -> BSONInteger(1)),
            BSONDocument("xyz" -> BSONInteger(2))
          )
      )
    )
  }

  it should "have an 'empty' resulting in no criteria" in {
    BSONDocument.pretty(Expression.empty) shouldBe BSONDocument.pretty(BSONDocument())
  }

  it should "ignore 'empty' in logical operators" in {
    BSONDocument.pretty(criteria.a === 1 && Expression.empty) shouldBe BSONDocument.pretty(
      BSONDocument("a" -> BSONInteger(1))
    )

    BSONDocument.pretty(Expression.empty && criteria.a === 2.0) shouldBe BSONDocument.pretty(
      BSONDocument("a" -> BSONDouble(2.0))
    )

    BSONDocument.pretty(Expression.empty || criteria.a === "three") shouldBe BSONDocument.pretty(
      BSONDocument("a" -> BSONString("three"))
    )

    BSONDocument.pretty(criteria.a === 4L || Expression.empty) shouldBe BSONDocument.pretty(
      BSONDocument("a" -> BSONLong(4L))
    )
  }
}
