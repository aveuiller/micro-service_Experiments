package com.experiments.carriers.entities

import akka.Done
import akka.actor.ActorSystem
import com.experiments.carriers.api.models.Location
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

  private val name = "name"
  private val age = 18
  private val licenses = List(LicenseType.A)
  private val siret = "siret"
  private val location = Location(1, 2, 3)

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

  "TrackCarrier" should {
    "Reject if carrier is not valid" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      val outcome = driver.run(TrackCarrier(location))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Add a new location to the carrier with default values" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      driver.run(AddCarrier(name, age, licenses, siret))
      val outcome = driver.run(TrackCarrier(location))

      outcome.events should ===(List(TrackingAdded(location)))
      outcome.state.name should ===(name)
      outcome.state.age should ===(age)
      outcome.state.ownedLicenses should ===(licenses)
      outcome.state.organizationSiret should ===(siret)
      outcome.state.location should ===(location)
      outcome.state.validated should ===(true)
      outcome.replies should ===(List(Done))
      outcome.issues should be(Nil)
    }
  }

  "GetCarrier" should {
    "Return the current carrier state with default location if not set" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      driver.run(AddCarrier(name, age, licenses, siret))
      val outcome = driver.run(GetCarrier)

      outcome.events should ===(List())
      outcome.state.name should ===(name)
      outcome.state.age should ===(age)
      outcome.state.ownedLicenses should ===(licenses)
      outcome.state.organizationSiret should ===(siret)
      outcome.state.location should ===(CarrierState.empty.location)
      outcome.replies should ===(List(CarrierState(name, age, licenses, siret, CarrierState.empty.location, validated = true)))
      outcome.issues should be(Nil)
    }

    "Return the current carrier state with tracked location if set" in {
      val driver = new PersistentEntityTestDriver(system, new CarrierEntity, "carrier-1")

      driver.run(AddCarrier(name, age, licenses, siret))
      driver.run(TrackCarrier(location))
      val outcome = driver.run(GetCarrier)

      outcome.events should ===(List())
      outcome.state.name should ===(name)
      outcome.state.age should ===(age)
      outcome.state.ownedLicenses should ===(licenses)
      outcome.state.organizationSiret should ===(siret)
      outcome.state.location should ===(location)
      outcome.replies should ===(List(CarrierState(name, age, licenses, siret, location, validated = true)))
      outcome.issues should be(Nil)
    }
  }
}
