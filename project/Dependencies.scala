import sbt._

object Dependencies {

  object v {
    val reactiveMongo = "1.0.6"
    val scala = "2.13.6"
  }

  object v2 {
    val catsEffect = "2.5.3"
    val fs2 = "2.5.9"
    val weaver = "0.6.4"
  }

  object v3 {
    val catsEffect = "3.2.2"
    val fs2 = "3.1.0"
    val weaver = "0.7.4"
  }

  val coreDependencies = Seq(
    "org.reactivemongo" %% "reactivemongo" % v.reactiveMongo,
    "org.reactivemongo" %% "reactivemongo-test" % "1.0.0" % Test
  )

  val ce3Dependencies = Seq(
    "org.typelevel" %% "cats-effect" % v3.catsEffect,
    "co.fs2" %% "fs2-core" % v3.fs2,
    "com.disneystreaming" %% "weaver-cats" % v3.weaver % Test
  )

  val ce2Dependencies = Seq(
    "org.typelevel" %% "cats-effect" % v2.catsEffect,
    "co.fs2" %% "fs2-core" % v2.fs2,
    "com.disneystreaming" %% "weaver-cats" % v2.weaver % Test
  )
}
