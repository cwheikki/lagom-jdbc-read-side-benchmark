import com.lightbend.lagom.core.LagomVersion

organization in ThisBuild := "com.cwheikki"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

val postgresDriver             = "org.postgresql"               % "postgresql"                                     % "42.2.8"
val macwire                    = "com.softwaremill.macwire"     %% "macros"                                        % "2.3.3" % "provided"
val scalaTest                  = "org.scalatest"                %% "scalatest"                                     % "3.1.1" % Test
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % "1.0.1"
val lagomScaladslAkkaDiscovery = "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current

lazy val `read-side-benchmark` = (project in file("."))
  .aggregate(`read-side-benchmark-api`, `read-side-benchmark-impl`)

lazy val `read-side-benchmark-api` = (project in file("read-side-benchmark-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `read-side-benchmark-impl` = (project in file("read-side-benchmark-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      postgresDriver,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`read-side-benchmark-api`)

// The project uses PostgreSQL
lagomCassandraEnabled in ThisBuild := false