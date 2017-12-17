package com.experiments.organizations.api.models

import play.api.libs.json.{Format, Json}

case class Carrier(name: String,
                   age: Int,
                   has_permis_a: Option[Boolean] = None,
                   has_permis_b: Option[Boolean] = None,
                   has_permis_c: Option[Boolean] = None,
                   id: Option[String] = None)

object Carrier {
  implicit val formatter: Format[Carrier] = Json.format[Carrier]
}