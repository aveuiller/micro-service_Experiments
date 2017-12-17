import sbt.Keys.libraryDependencies

name := "transporterOrganizations-root"
organization in ThisBuild := "com.experiments"
version in ThisBuild := "1.0.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

lazy val root = (project in file("."))
  .aggregate(`carriers-api`, carriers, `organizations-api`, organizations)

lazy val carriers = project
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`carriers-api`)

lazy val `carriers-api` = project
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      "com.typesafe.play" %% "play-json-joda" % "2.6.8"
    )
  )

lazy val organizations = project
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`carriers-api`, `organizations-api`)

lazy val `organizations-api` = project
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )