package com.experiments.carriers.service

import java.util.UUID

import akka.{Done, NotUsed}
import com.experiments.carriers.api.events.CarrierCreated
import com.experiments.carriers.api.models.{Carrier, Location}
import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.carriers.entities.{AddCarrier, CarrierAdded, CarrierEntity, CarrierEvent, GetCarrier, LicenseType}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

import scala.concurrent.{ExecutionContext, Future}

class CarriersService(persistentEntityRegistry: PersistentEntityRegistry)
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
        Some(state.hasALicense), Some(state.hasBLicense), Some(state.hasCLicense)
      )
    }
  }

  override def track(id: String): ServiceCall[Location, Done] = ServiceCall { location =>
    // TODO: Track user
    Future.successful(Done)
  }

  override def carrierCreatedTopic(): Topic[CarrierCreated] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(CarrierEvent.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  /**
   * Create a [[CarrierCreated]] from any given [[com.experiments.carriers.entities.CarrierAdded]] event.
   *
   * @param event The event to convert.
   * @return The converted event.
   */
  private def convertEvent(event: EventStreamElement[CarrierEvent]): CarrierCreated = {
    event.event match {
      case CarrierAdded(name, age, ownedLicense, organizationSiret) =>
        CarrierCreated(event.entityId, organizationSiret)
    }
  }
}
