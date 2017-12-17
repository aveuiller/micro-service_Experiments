package com.experiments.organizations

import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.organizations.api.service.OrganizationsServiceApi
import com.experiments.organizations.entities.{OrganizationEntity, OrganizationSerializerRegistry}
import com.experiments.organizations.service.OrganizationsService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraPersistenceComponents, ReadSideCassandraPersistenceComponents}
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

abstract class OrganizationsApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaClientComponents
    with ReadSideCassandraPersistenceComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[OrganizationsServiceApi](wire[OrganizationsService])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = OrganizationSerializerRegistry

  // Register the hello-lagom persistent entity
  persistentEntityRegistry.register(wire[OrganizationEntity])

  lazy val carriersService = serviceClient.implement[CarriersServiceApi]
}
