package com.experiments.organizations.entities

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import scala.collection.immutable


/**
 * Akka serialization, used by both persistence and remoting, needs to have
 * serializers registered for every type serialized or deserialized. While it's
 * possible to use any serializer you want for Akka messages, out of the box
 * Lagom provides support for JSON, via this registry abstraction.
 *
 * The serializers are registered here, and then provided to Lagom in the
 * application loader.
 */
object OrganizationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] = immutable.Seq(
    // Commands
    JsonSerializer[AddOrganization],
    JsonSerializer[AddCarrier],
    JsonSerializer[GetCarriers.type], // Dummy format to avoid serialization crash in unit tests
    JsonSerializer[GetOrganization.type], // Dummy format to avoid serialization crash in unit tests
    // Events
    JsonSerializer[OrganizationAdded],
    JsonSerializer[CarrierAdded],
    // States
    JsonSerializer[OrganizationState],
    JsonSerializer[Carriers]
  )
}