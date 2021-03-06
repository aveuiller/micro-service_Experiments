package com.experiments.organizations.api.service

import akka.{Done, NotUsed}
import com.experiments.organizations.api.models.{Carrier, GroupedOrganizations, Organization}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait OrganizationsServiceApi extends Service {

  def create(): ServiceCall[Organization, Done]

  def carriers(siret: String): ServiceCall[NotUsed, List[Carrier]]

  def groupByPostalCodes():ServiceCall[NotUsed, List[GroupedOrganizations]]

  override final def descriptor = {
    import com.lightbend.lagom.scaladsl.api.Service._
    // @formatter:off
    named("organizations")
      .withCalls(
        restCall(Method.POST, "/organizations", create _),
        restCall(Method.GET, "/organizations/:siret/carriers", carriers _),
        pathCall("/organizations/by_postal_codes", groupByPostalCodes _)
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
