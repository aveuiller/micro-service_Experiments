package com.experiments.organizations.entities

import play.api.libs.json.{Format, Json}

object OrganizationState {
  val empty = OrganizationState("", List(), List(), validated = false)

  implicit val format: Format[OrganizationState] = Json.format[OrganizationState]

}

final case class OrganizationState(name: String,
                                   postalCode: List[String],
                                   carriers: List[String],
                                   validated: Boolean)
