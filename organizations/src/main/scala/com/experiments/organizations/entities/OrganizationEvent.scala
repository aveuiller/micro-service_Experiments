package com.experiments.organizations.entities

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import play.api.libs.json.{Format, Json}

object OrganizationEvent {
  val Tag = AggregateEventTag[OrganizationEvent]
}

trait OrganizationEvent extends AggregateEvent[OrganizationEvent] {
  override def aggregateTag: AggregateEventTagger[OrganizationEvent] = OrganizationEvent.Tag
}

final case class OrganizationAdded(name: String,
                                   postalCodes: List[String]) extends OrganizationEvent

object OrganizationAdded {
  implicit val format: Format[OrganizationAdded] = Json.format
}

final case class CarrierAdded(carrierId: String) extends OrganizationEvent

object CarrierAdded {
  implicit val format: Format[CarrierAdded] = Json.format
}