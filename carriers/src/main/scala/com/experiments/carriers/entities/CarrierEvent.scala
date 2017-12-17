package com.experiments.carriers.entities

import com.experiments.carriers.api.models.Location
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}

object CarrierEvent {
  val Tag = AggregateEventTag[CarrierEvent]
}

sealed trait CarrierEvent extends AggregateEvent[CarrierEvent] {
  def aggregateTag = CarrierEvent.Tag
}

final case class CarrierAdded(name: String,
                              age: Int,
                              ownedLicense: Seq[LicenseType.Value],
                              organizationSiret: String) extends CarrierEvent

object CarrierAdded {
  implicit val format: Format[CarrierAdded] = Json.format
}

final case class TrackingAdded(location: Location) extends CarrierEvent

object TrackingAdded {
  implicit val format: Format[CarrierAdded] = Json.format
}