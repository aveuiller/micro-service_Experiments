import sbt.Keys.resolvers

name := "transporterOrganizations-root"

lazy val root = (project in file("."))
  .settings(commonSettings)
  .dependsOn(carriers, organizations)
  .aggregate(carriers, organizations)

lazy val commonSettings = Seq(
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.12.4",
  resolvers ++= Seq(
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/")
)

libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice)

lazy val carriers = project
  .settings(commonSettings)
  .enablePlugins(PlayScala)

lazy val organizations = project
  .settings(commonSettings)
  .enablePlugins(PlayScala)
