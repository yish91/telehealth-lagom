import Dependencies._

organization in ThisBuild := "com.fss"
version in ThisBuild := "0.1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

lazy val telehealth = (project in file("."))
  .aggregate(`doctor-api`, `doctor-impl`, `patient-api`, `patient-impl`)

lazy val `doctor-api` = (project in file("doctor-api"))
.settings(
  libraryDependencies ++= Seq(
    lagomScaladslApi
  )
)

lazy val `doctor-impl` = (project in file("doctor-impl"))
.enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      filters
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`doctor-api`)

lazy val `patient-api` = (project in file("patient-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `patient-impl` = (project in file("patient-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      filters
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`patient-api`)

lazy val `queue-api` = (project in file("queue-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `queue-impl` = (project in file("queue-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      filters
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`queue-api`)

/*lazy val `doctor-stream-api` = (project in file("doctor-stream-api"))
.settings(
  libraryDependencies ++= Seq(
    lagomScaladslApi
  )
)

lazy val `doctor-stream-impl` = (project in file("doctor-stream-impl"))
.enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`doctor-stream-api`, `doctor-api`)*/

lagomKafkaEnabled in ThisBuild := false
lagomCassandraEnabled in ThisBuild := false
