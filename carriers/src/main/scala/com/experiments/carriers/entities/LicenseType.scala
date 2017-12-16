package com.experiments.carriers.entities

import com.experiments.carriers.utils.EnumUtils
import play.api.libs.json.Format

object LicenseType extends Enumeration {
  val A, B, C = Value

  private val activationArray = List(A, B, C)

  implicit val format: Format[LicenseType.Value] = EnumUtils.enumFormat(this)

  def licenseSequence(licenseA: Boolean, licenseB: Boolean, licenseC: Boolean): List[LicenseType.Value] = {
    val toActivate = List(licenseA, licenseB, licenseC)
    (toActivate, activationArray).zipped.filter({ (activated, _) => activated })._2
  }
}
