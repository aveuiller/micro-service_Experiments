package com.experiments.carriers.api.events

import play.api.libs.json.{Format, Json}

case class CarrierCreated(id: String, organizationSiret: String)

object CarrierCreated {
  implicit val format: Format[CarrierCreated] = Json.format[CarrierCreated]
}