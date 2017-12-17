package com.experiments.carriers.api.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JodaReads, JodaWrites, JsPath}

case class Location(latitude: Double, longitude: Double, altitude: Double, timestamp: DateTime = DateTime.now())

object Location {
  implicit val dateTimeFormat: Format[DateTime] = Format(
    JodaReads.DefaultJodaDateTimeReads,
    JodaWrites.JodaDateTimeWrites
  )

  implicit val format: Format[Location] = (
    (JsPath \ "latitude").format[Double] and
      (JsPath \ "longitude").format[Double] and
      (JsPath \ "altitude").format[Double] and
      (JsPath \ "timestamp").formatWithDefault(DateTime.now())
    ) (Location.apply, unlift(Location.unapply))
}
