package com.experiments.carriers.entities

import play.api.libs.json.{Format, Json}

object CarrierState {
  implicit val licenseTypeFormat = LicenseType.format
  implicit val format: Format[CarrierState] = Json.format

  val empty = CarrierState("", 0, Seq(), "")
}

final case class CarrierState(name: String,
                              age: Int,
                              ownedLicenses: Seq[LicenseType.Value],
                              organizationSiret: String) {
  def hasALicense: Boolean = ownedLicenses.contains(LicenseType.A)

  def hasBLicense: Boolean = ownedLicenses.contains(LicenseType.B)

  def hasCLicense: Boolean = ownedLicenses.contains(LicenseType.C)
}
