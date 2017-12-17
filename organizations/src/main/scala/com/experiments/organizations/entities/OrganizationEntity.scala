package com.experiments.organizations.entities

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

class OrganizationEntity extends PersistentEntity {
  override type Command = OrganizationCommand[_]
  override type Event = OrganizationEvent
  override type State = OrganizationState

  override def initialState: State = OrganizationState.empty

  override def behavior: Behavior = Actions()
    .onCommand[AddOrganization, Done] {
    case (AddOrganization(name, postalCodes, carriers), ctx, _) =>
      if (name == null || name.isEmpty) {
        ctx.invalidCommand("The organization name should not be empty!")
        ctx.done
      } else if (postalCodes.isEmpty) {
        ctx.invalidCommand("The organization should at least deliver one postal code!")
        ctx.done
      } else if (carriers.isEmpty) {
        ctx.invalidCommand("The organization should at least have one carrier!")
        ctx.done
      } else {
        ctx.thenPersist(OrganizationAdded(name, postalCodes)) { _ =>
          ctx.reply(Done)
        }
      }
  }
    .onCommand[AddCarrier, Done] {
    case (AddCarrier(carrierId), ctx, _) =>
      ctx.thenPersist(CarrierAdded(carrierId)) { _ =>
        ctx.reply(Done)
      }
  }
    .onReadOnlyCommand[GetCarriers.type, Carriers] {
    case (GetCarriers, ctx, state) => ctx.reply(Carriers(state.carriers))
  }
    .onReadOnlyCommand[GetOrganization.type, OrganizationState] {
    case (GetOrganization, ctx, state) => ctx.reply(state)
  }
    .onEvent {
      case (CarrierAdded(carrierId), state) =>
        state.copy(carriers = state.carriers :+ carrierId, validated = true)
    }
    .onEvent {
      case (OrganizationAdded(name, postalCodes), state) =>
        state.copy(name, postalCodes)
    }
}