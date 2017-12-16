package com.experiments.carriers.entities

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

class CarrierEntity extends PersistentEntity {
  override type Command = CarrierCommand[_]
  override type Event = CarrierEvent
  override type State = CarrierState

  override def initialState: State = CarrierState.empty

  override def behavior: Behavior = Actions()
    .onCommand[AddCarrier, AddCarrierDone] {
    case (AddCarrier(name, age, ownedLicenses, organizationSiret), ctx, state) =>
      if (name == null || name.isEmpty) {
        ctx.invalidCommand("The carrier name should not be empty!")
        ctx.done
      } else if (age < 18) {
        ctx.invalidCommand("The carrier must be major to be employed.")
        ctx.done
      } else if (ownedLicenses.isEmpty) {
        ctx.invalidCommand("The carrier should own at least one driving license.")
        ctx.done
      } else {
        ctx.thenPersist(CarrierAdded(name, age, ownedLicenses, organizationSiret)) { _ =>
          ctx.reply(AddCarrierDone(entityId))
        }
      }
  }
    .onEvent {
      case (CarrierAdded(name, age, ownedLicense, organizationSiret), state) =>
        CarrierState(name, age, ownedLicense, organizationSiret)
    }
    .onReadOnlyCommand[GetCarrier.type, CarrierState] {
    case (GetCarrier, ctx, state) => ctx.reply(state)
  }
}

