package com.experiments.carriers

import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.carriers.entities.{CarrierEntity, CarrierSerializerRegistry}
import com.experiments.carriers.service.CarriersService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

abstract class CarriersApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[CarriersServiceApi](wire[CarriersService])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = CarrierSerializerRegistry

  // Register the hello-lagom persistent entity
  persistentEntityRegistry.register(wire[CarrierEntity])
}
