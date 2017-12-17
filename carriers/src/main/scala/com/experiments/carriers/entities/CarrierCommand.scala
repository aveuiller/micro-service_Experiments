package com.experiments.carriers.entities

import akka.Done
import com.experiments.carriers.api.models.Location
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, JsSuccess, Json, Reads, Writes}

sealed trait CarrierCommand[R] extends ReplyType[R]

final case class AddCarrier(name: String,
                            age: Int,
                            ownedLicenses: Seq[LicenseType.Value],
                            organizationSiret: String) extends CarrierCommand[AddCarrierDone]

object AddCarrier {
  implicit val format: Format[AddCarrier] = Json.format
}

final case class AddCarrierDone(id: String)

object AddCarrierDone {
  implicit val format: Format[AddCarrierDone] = Json.format
}

case object GetCarrier extends CarrierCommand[CarrierState] {
  // Dummy format to avoid serialization crash in unit tests
  implicit val format: Format[GetCarrier.type] = Format(
    Reads[GetCarrier.type](_ => JsSuccess(GetCarrier)),
    Writes[GetCarrier.type](_ => Json.obj())
  )
}

case object GetLocation extends CarrierCommand[Location] {
  // Dummy format to avoid serialization crash in unit tests
  implicit val format: Format[GetLocation.type] = Format(
    Reads[GetLocation.type](_ => JsSuccess(GetLocation)),
    Writes[GetLocation.type](_ => Json.obj())
  )
}

final case class TrackCarrier(Location: Location) extends CarrierCommand[Done]
