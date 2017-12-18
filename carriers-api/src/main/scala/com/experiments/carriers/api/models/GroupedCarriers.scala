package com.experiments.carriers.api.models

import play.api.libs.json.{Format, Json}

/**
 * Contains the ID of organizations available in one postal code.
 *
 * @param postalCode The postal code
 * @param carriers   The carriers available for this postal code.
 */
case class GroupedCarriers(postalCode: String, carriers: List[String])

object GroupedCarriers {
  implicit val format: Format[GroupedCarriers] = Json.format
}