package com.experiments.organizations.api.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}

/**
 * Define a transporter organization.
 *
 * @param name        mandatory and non empty
 * @param siret       mandatory and unique
 * @param postalCodes mandatory and non empty
 * @param carriers    mandatory and non empty
 */
case class Organization(
                         name: String,
                         siret: String,
                         postalCodes: List[String],
                         carriers: List[Carrier]
                       )

object Organization {
  implicit val formatter: Format[Organization] = (
    (JsPath \ "name").format[String] and
      (JsPath \ "SIRET").format[String] and
      (JsPath \ "postal_codes").format[List[String]] and
      (JsPath \ "carriers").format[List[Carrier]]
    ) (Organization.apply, unlift(Organization.unapply))

}
