package com.experiments.carriers.entities

import com.experiments.carriers.api.models.Location
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}

sealed trait CarrierEvent

final case class CarrierAdded(name: String,
                              age: Int,
                              ownedLicense: Seq[LicenseType.Value],
                              organizationSiret: String) extends CarrierEvent with AggregateEvent[CarrierAdded] {
  override def aggregateTag = CarrierAdded.Tag
}

object CarrierAdded {
  val Tag = AggregateEventTag[CarrierAdded]
  implicit val format: Format[CarrierAdded] = Json.format
}

final case class TrackingAdded(location: Location) extends CarrierEvent

object TrackingAdded {
  implicit val format: Format[TrackingAdded] = Json.format
}