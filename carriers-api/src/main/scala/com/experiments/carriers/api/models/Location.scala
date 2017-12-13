package com.experiments.carriers.api.models

import play.api.libs.json.{Format, Json}

case class Location(latitude: Double, longitude: Double, altitude: Double)

object Location {
  implicit val format: Format[Location] = Json.format
}
