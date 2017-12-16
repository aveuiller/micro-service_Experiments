package com.experiments.organizations.service

import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.experiments.carriers.api.events.CarrierCreated
import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.organizations.api.models.{Carrier, Organization}
import com.experiments.organizations.api.service.OrganizationsServiceApi
import com.experiments.organizations.entities.{AddCarrier, AddOrganization, Carriers, GetCarriers, OrganizationEntity}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.{ExecutionContext, Future}

class OrganizationsService(carrierService: CarriersServiceApi,
                           persistentEntityRegistry: PersistentEntityRegistry)
                          (implicit ec: ExecutionContext) extends OrganizationsServiceApi {

  // Subscribe to carrier creation.
  carrierService.carrierCreatedTopic().subscribe
    .atLeastOnce(
      Flow[CarrierCreated].map { msg =>
        val ref = persistentEntityRegistry.refFor[OrganizationEntity](msg.organizationSiret)
        ref.ask(AddCarrier(msg.id))
        Done
      }
    )

  override def create(): ServiceCall[Organization, Done] = ServiceCall { organization =>
    if (organization.siret.isEmpty) {
      throw BadRequest("The organization SIRET should not be empty!")
    }
    val ref = persistentEntityRegistry.refFor[OrganizationEntity](organization.siret)
    ref.ask(AddOrganization(organization.name, organization.postalCodes, organization.carriers))
      .map(_ => Done)
      .recoverWith({
        case e: InvalidCommandException => throw BadRequest(e.message)
      }).andThen({ case _ => createCarriers(organization) })

  }

  /**
   * Call [[CarriersServiceApi.create()]] for each carrier contained in the organization.
   *
   * It will then wait for the carrier creation confirmation from [[CarrierCreated]]
   * in order to add them to the organization.
   *
   * @param organization The organization to create carriers for.
   * @return Asynchronous method returning nothing.
   */
  private def createCarriers(organization: Organization): Future[Unit] = {
    organization.carriers.map(carrier =>
      carrierService.create().invoke(
        com.experiments.carriers.api.models.Carrier(
          carrier.name, carrier.age, organization.siret,
          carrier.has_permis_a, carrier.has_permis_b, carrier.has_permis_c
        )
      )
    )
    Future.successful(())
  }

  override def carriers(siret: String): ServiceCall[NotUsed, List[Carrier]] = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[OrganizationEntity](siret)
    ref.ask(GetCarriers) flatMap { carriers: Carriers =>
      val carrierList = for (id <- carriers.ids) yield carrierService.fetch(id).invoke()
      Future.sequence(carrierList)
        .map(
          _.map(fromApi =>
            Carrier(
              fromApi.name, fromApi.age,
              fromApi.has_permis_a, fromApi.has_permis_b, fromApi.has_permis_c
            )
          )
        )
    }
  }
}
