import sbt._

object Dependencies {

  object v {
    val catsEffect = "3.2.0"
    val reactiveMongo = "1.0.6"
    val fs2 = "3.0.6"
    val scala = "2.13.6"
  }

  val coreDependencies = Seq(
    "org.typelevel" %% "cats-effect" % v.catsEffect,
    "org.reactivemongo" %% "reactivemongo" % v.reactiveMongo,
    "co.fs2" %% "fs2-core" % v.fs2,
  )
}
