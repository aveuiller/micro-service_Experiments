package com.experiments.carriers.service

import java.util.UUID

import akka.{Done, NotUsed}
import com.experiments.carriers.api.events.CarrierCreated
import com.experiments.carriers.api.models.{Carrier, GroupedCarriers, Location}
import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.carriers.entities.{AddCarrier, CarrierAdded, CarrierEntity, GetCarrier, LicenseType, TrackCarrier}
import com.experiments.organizations.api.service.OrganizationsServiceApi
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

import scala.concurrent.{ExecutionContext, Future}

class CarriersService(persistentEntityRegistry: PersistentEntityRegistry,
                      organizationsService: OrganizationsServiceApi
                     )
                     (implicit ec: ExecutionContext) extends CarriersServiceApi {
  override def create(): ServiceCall[Carrier, Carrier] = ServiceCall { carrier =>
    // TODO: Handle possible ID collision
    val identifier = UUID.randomUUID().toString
    val ref = persistentEntityRegistry.refFor[CarrierEntity](identifier)
    ref.ask(AddCarrier(
      carrier.name, carrier.age,
      LicenseType.licenseSequence(
        carrier.has_permis_a.getOrElse(false),
        carrier.has_permis_b.getOrElse(false),
        carrier.has_permis_c.getOrElse(false)
      ),
      carrier.organizationSiret
    )).map(_ => carrier.copy(id = Some(identifier)))
      .recoverWith({
        case e: InvalidCommandException => throw BadRequest(e.message)
      })
  }

  override def fetch(id: String): ServiceCall[NotUsed, Carrier] = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[CarrierEntity](id)
    ref.ask(GetCarrier) map { state =>
      Carrier(
        state.name, state.age, state.organizationSiret,
        Some(state.hasALicense), Some(state.hasBLicense), Some(state.hasCLicense),
        Some(id)
      )
    }
  }

  override def track(id: String): ServiceCall[Location, Done] = ServiceCall { location =>
    val ref = persistentEntityRegistry.refFor[CarrierEntity](id)
    ref.ask(TrackCarrier(location))
      .map({ _ => Done })
      .recoverWith({
        case e: InvalidCommandException => throw BadRequest(e.message)
      })
  }

  def getLocation(id: String): ServiceCall[NotUsed, Location] = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[CarrierEntity](id)
    ref.ask(GetCarrier) map (_.location)
  }

  override def carrierCreatedTopic(): Topic[CarrierCreated] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(CarrierAdded.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  /**
   * Create a [[CarrierCreated]] from any given [[com.experiments.carriers.entities.CarrierAdded]] event.
   *
   * @param event The event to convert.
   * @return The converted event.
   */
  private def convertEvent(event: EventStreamElement[CarrierAdded]): CarrierCreated = {
    event.event match {
      case CarrierAdded(name, age, ownedLicense, organizationSiret) =>
        CarrierCreated(event.entityId, organizationSiret)
    }
  }

  override def groupByPostalCodes(): ServiceCall[NotUsed, List[GroupedCarriers]] = ServiceCall { _ =>
    organizationsService.groupByPostalCodes().invoke().flatMap { groupedOrganizations =>
      Future.sequence(
        groupedOrganizations.map { groupedOrganization =>
          retrieveCarriersForOrganizations(groupedOrganization.organizations) map { carriers =>
            GroupedCarriers(groupedOrganization.postalCode, carriers.flatMap(_.id).distinct)
          }
        })
    }
  }

  // TODO: We can optimize network communications by creating a carriers(siret: id) method in this service.
  private def retrieveCarriersForOrganizations(organizations: List[String]): Future[List[com.experiments.organizations.api.models.Carrier]] = {
    Future.sequence(
      organizations.map(organizationsService.carriers(_).invoke())
    ).map(_.flatten)
  }
}
