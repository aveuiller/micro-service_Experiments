package com.experiments.organizations.entities

import akka.Done
import com.experiments.organizations.api.models.Carrier
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, JsSuccess, Json, Reads, Writes}

sealed trait OrganizationCommand[R] extends ReplyType[R]

final case class AddOrganization(name: String,
                                 postalCodes: List[String],
                                 carriers: List[Carrier]
                                ) extends OrganizationCommand[Done]

object AddOrganization {
  implicit val format: Format[AddOrganization] = Json.format
}

final case class AddCarrier(carrierId: String) extends OrganizationCommand[Done]

object AddCarrier {
  implicit val format: Format[AddCarrier] = Json.format
}

case object GetOrganization extends OrganizationCommand[OrganizationState] {
  // Dummy format to avoid serialization crash in unit tests
  implicit val format: Format[GetOrganization.type] = Format(
    Reads[GetOrganization.type](_ => JsSuccess(GetOrganization)),
    Writes[GetOrganization.type](_ => Json.obj())
  )
}

case object GetCarriers extends OrganizationCommand[Carriers] {
  // Dummy format to avoid serialization crash in unit tests
  implicit val format: Format[GetCarriers.type] = Format(
    Reads[GetCarriers.type](_ => JsSuccess(GetCarriers)),
    Writes[GetCarriers.type](_ => Json.obj())
  )
}

case class Carriers(ids: List[String])

object Carriers {
  implicit val format: Format[Carriers] = Json.format
}