import java.util.Date

import Dependencies._

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addCommandAlias("f", ";scalafixAll;scalafmtAll")

val ce3Ver = "0.1.0-SNAPSHOT"
val ce2Ver = "0.1.0-SNAPSHOT"
val dslVer = "0.0.1-SNAPSHOT"

def scalafixRunExplicitly: Def.Initialize[Task[Boolean]] =
  Def.task {
    executionRoots.value.exists { root =>
      Seq(
        scalafix.key,
        scalafixAll.key
      ).contains(root.key)
    }
  }

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .aggregate(ce2, ce3, core, dsl)

lazy val core = (project in file("modules/core"))
  .settings(
    commonSettings,
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
    libraryDependencies ++= coreDependencies,
  )

lazy val commonSettings = Seq(
  scalacOptions += "-Ywarn-unused",
  scalaVersion := "2.13.5",
  organization := "com.bcf",
  organizationName := "Bluechip Financial",
  addCompilerPlugin(scalafixSemanticdb),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.+" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.+"),
  ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.+",
  Compile / scalacOptions ++= Seq(
    "-Ymacro-annotations",
  ),
  scalacOptions --= {
    if (!scalafixRunExplicitly.value) Seq() else Seq("-Xfatal-warnings")
  },
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  //Exclude ScalaTest brought in from transitive dependencies
  testFrameworks -= TestFrameworks.ScalaTest
)

lazy val ce2 = (project in file("modules/ce2"))
  .settings(
    name := "reactivemongo4s-ce2",
    version := ce2Ver,
    commonSettings,
    libraryDependencies ++= coreDependencies ++ ce2Dependencies,
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
  ).dependsOn(core % "compile->compile;test->test")

lazy val ce3 = (project in file("modules/ce3"))
  .settings(
    name := "reactivemongo4s-ce3",
    version := ce3Ver,
    commonSettings,
    libraryDependencies ++= coreDependencies ++ ce3Dependencies,
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
  ).dependsOn(core % "compile->compile;test->test")

lazy val dsl = (project in file("modules/dsl"))
  .settings(
    commonSettings,
    name := "reactivemongo4s-dsl",
    version := dslVer,
    libraryDependencies ++= coreDependencies ++ dslDependencies,
    testFrameworks += TestFrameworks.ScalaTest,
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    }
  )
