import Dependencies._
import java.util.Date
import sbtgitflowversion.BranchMatcher._
import sbtgitflowversion.VersionCalculator._

addCommandAlias("f", "scalafmtAll")

lazy val ce2 = (project in file("modules/ce2"))
  .settings(
    name := "reactivemongo4s",
    organization := "com.bcf",
    organizationName := "Bluechip Financial",
    version := "0.2-SNAPSHOT",
    scalaVersion := v.scala,
    libraryDependencies ++= coreDependencies,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    // auto-generate version based on git
    tagMatcher := TagMatcher.prefix("v"),
    versionPolicy := Seq(
      exact("master") -> currentTag(),
      exact("develop") -> nextMinor(),
      prefix("release/v") -> matching(),
      prefixes("feature/", "bugfix/", "hotfix/") -> lastVersionWithMatching(),
      any -> currentTag()
    ),
    Global / excludeLintKeys ++= Set(tagMatcher, versionPolicy),
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
  )

lazy val ce3 = (project in file("modules/ce3"))
  .settings(
    name := "reactivemongo4s",
    organization := "com.bcf",
    organizationName := "Bluechip Financial",
    version := "0.3-SNAPSHOT",
    scalaVersion := v.scala,
    libraryDependencies ++= coreDependencies,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    // auto-generate version based on git
    tagMatcher := TagMatcher.prefix("v"),
    versionPolicy := Seq(
      exact("master") -> currentTag(),
      exact("develop") -> nextMinor(),
      prefix("release/v") -> matching(),
      prefixes("feature/", "bugfix/", "hotfix/") -> lastVersionWithMatching(),
      any -> currentTag()
    ),
    Global / excludeLintKeys ++= Set(tagMatcher, versionPolicy),
    // releases
    publishTo := {
      val base = "https://bluechipfinancial.jfrog.io/artifactory/sbt-release-local"
      if (isSnapshot.value) Some("Artifactory Realm" at base + ";build.timestamp=" + new Date().getTime)
      else Some("Artifactory Realm" at base)
    },
  )