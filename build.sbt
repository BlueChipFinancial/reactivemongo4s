name := "reactivemongo4cats"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.6"

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.2.0"

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "1.0.6"
libraryDependencies += "co.fs2" %% "fs2-core" % "3.0.6"
//libraryDependencies += "ch.qos.logback"  % "logback-classic" % "1.2.5"

idePackagePrefix := Some("com.bcf")
