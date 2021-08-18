import sbt._

object Dependencies {

  object v {
    val catsEffect = "2.5.3"
    val reactiveMongo = "1.0.6"
    val fs2 = "2.5.9"
    val scala = "2.13.6"
  }

  val coreDependencies = Seq(
    "org.typelevel" %% "cats-effect" % v.catsEffect,
    "org.reactivemongo" %% "reactivemongo" % v.reactiveMongo,
    "co.fs2" %% "fs2-core" % v.fs2,
  )
}
