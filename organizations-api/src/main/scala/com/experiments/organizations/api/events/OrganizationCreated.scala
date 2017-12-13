package com.experiments.organizations.api.events

import com.experiments.organizations.api.models.Carrier
import play.api.libs.json.{Format, Json}

case class OrganizationCreated(siret: String, carriers: List[Carrier])

object OrganizationCreated {
  implicit val format: Format[OrganizationCreated] = Json.format[OrganizationCreated]
}