package com.experiments.carriers.entities

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
object CarrierSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] = immutable.Seq(
    // Commands
    JsonSerializer[AddCarrier],
    JsonSerializer[AddCarrierDone],
    JsonSerializer[GetCarrier.type], // Dummy format to avoid serialization crash in unit tests
    // Events
    JsonSerializer[CarrierAdded],
    // States
    JsonSerializer[CarrierState],
    JsonSerializer[LicenseType.Value]
  )
}