package com.experiments.organizations.api.models

import play.api.libs.json.{Format, Json}

/**
 * Contains the SIRET of organizations available in one postal code.
 *
 * @param postalCode    The postal code
 * @param organizations The organizations available for this postal code.
 */
case class GroupedOrganizations(postalCode: String, organizations: List[String])

object GroupedOrganizations {
  implicit val format: Format[GroupedOrganizations] = Json.format
}