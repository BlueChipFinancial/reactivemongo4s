import java.util.Date

import Dependencies._
import sbtgitflowversion.BranchMatcher._
import sbtgitflowversion.VersionCalculator._

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addCommandAlias("f", ";scalafixAll;scalafmtAll")

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
  .aggregate(ce2, ce3, core)

lazy val core = (project in file ("modules/core"))
  .settings(
    commonSettings,
    noPublishSettings,
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
  testFrameworks -= TestFrameworks.ScalaTest,
  // auto-generate version based on git
  tagMatcher := TagMatcher.prefix("v"),
  versionPolicy := Seq(
    exact("master") -> currentTag(),
    exact("develop") -> nextMinor(),
    prefix("release/v") -> matching(),
    prefixes("feature/", "bugfix/", "hotfix/") -> lastVersionWithMatching(),
    any -> currentTag()
  ),
  Global / excludeLintKeys ++= Set(tagMatcher, versionPolicy)
)

lazy val ce2 = (project in file("modules/ce2"))
  .settings(
    name := "reactivemongo4s",
    version := "0.1.1",
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
    name := "reactivemongo4s",
    version := "0.2.1",
    commonSettings,
    libraryDependencies ++= coreDependencies ++ ce3Dependencies,
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
  ).dependsOn(core % "compile->compile;test->test")
