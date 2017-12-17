package com.experiments.carriers.entities

import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class CarrierEntitySpec extends WordSpecLike with Matchers with BeforeAndAfterAll
  with TypeCheckedTripleEquals {
  val system = ActorSystem("CarrierEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(CarrierSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "AddCarrier" should {

    "Reject if name is empty" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      val outcome = driver.run(AddCarrier("", 18, List(LicenseType.A), "siret"))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Reject if age is < 18" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      val outcome = driver.run(AddCarrier("name", 17, List(LicenseType.A), "siret"))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Reject if licenses list is empty" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      val outcome = driver.run(AddCarrier("name", 18, List(), "siret"))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Add a new carrier values if valid" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")
      val name = "name"
      val age = 18
      val licenses = List(LicenseType.A)
      val siret = "siret"

      val outcome = driver.run(AddCarrier(name, age, licenses, siret))

      outcome.events should ===(List(CarrierAdded(name, age, licenses, siret)))
      outcome.state.name should ===(name)
      outcome.state.age should ===(age)
      outcome.state.ownedLicenses should ===(licenses)
      outcome.state.organizationSiret should ===(siret)
      outcome.replies should ===(List(AddCarrierDone("carrier-1")))
      outcome.issues should be(Nil)
    }

  }

  "GetCarrier" should {
    "Return the current carrier state" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")
      val name = "name"
      val age = 18
      val licenses = List(LicenseType.A)
      val siret = "siret"

      driver.run(AddCarrier(name, age, licenses, siret))
      val outcome = driver.run(GetCarrier)

      outcome.events should ===(List())
      outcome.state.name should ===(name)
      outcome.state.age should ===(age)
      outcome.state.ownedLicenses should ===(licenses)
      outcome.state.organizationSiret should ===(siret)
      outcome.replies should ===(List(CarrierState(name, age, licenses, siret, CarrierState.empty.location)))
      outcome.issues should be(Nil)
    }
  }
}
