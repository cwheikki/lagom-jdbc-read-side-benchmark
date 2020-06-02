package com.cwheikki.readsidebenchmark.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.cwheikki.readsidebenchmark.api.ReadsidebenchmarkService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

class ReadsidebenchmarkLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ReadsidebenchmarkApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new ReadsidebenchmarkApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[ReadsidebenchmarkService])
}

abstract class ReadsidebenchmarkApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with SlickPersistenceComponents
    with HikariCPComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ReadsidebenchmarkService](wire[ReadsideBenchmarkServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ReadsidebenchmarkSerializerRegistry

  lazy val readSideBenchmarkRepository = wire[ReadSideBenchmarkRepository]
  readSide.register(wire[ReadSideBenchmarkProcessor])

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(ReadsidebenchmarkState.typeKey)(
      entityContext => ReadsidebenchmarkBehavior.create(entityContext)
    )
  )

}
