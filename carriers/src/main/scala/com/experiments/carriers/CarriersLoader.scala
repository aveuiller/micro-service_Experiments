package com.experiments.carriers

import com.experiments.carriers.api.service.CarriersServiceApi
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._

class CarriersLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new CarriersApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new CarriersApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[CarriersServiceApi])
}


