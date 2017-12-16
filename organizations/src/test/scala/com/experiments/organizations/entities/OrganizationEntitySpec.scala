package com.experiments.organizations.entities

import akka.Done
import akka.actor.ActorSystem
import com.experiments.organizations.api.models.Carrier
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class OrganizationEntitySpec extends WordSpecLike with Matchers with BeforeAndAfterAll
  with TypeCheckedTripleEquals {
  val system = ActorSystem("OrganizationEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(OrganizationSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "AddCarrier" should {
    "Add a new carrier into the Organization and validate it" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val carrierId = "id"

      val outcome = driver.run(AddCarrier(carrierId))

      outcome.events should ===(List(CarrierAdded(carrierId)))
      outcome.state.validated should ===(true)
      outcome.state.carriers should ===(List(carrierId))
      outcome.replies should ===(List(Done))
      outcome.issues should be(Nil)
    }

  }

  "AddOrganization" should {
    "set the name and postal codes" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val orgaName = "name"
      val postalCodes = List("59000", "76000")
      val carriers = List(Carrier("name", 18, Some(true)))

      val outcome = driver.run(AddOrganization(orgaName, postalCodes, carriers))

      outcome.events should ===(List(OrganizationAdded(orgaName, postalCodes)))
      outcome.state.validated should ===(false)
      outcome.state.postalCode should ===(postalCodes)
      outcome.replies should ===(List(Done))
      outcome.issues should be(Nil)
    }

    "Reject if name is empty" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val postalCodes = List("59000", "76000")
      val carriers = List(Carrier("name", 18, Some(true)))

      val outcome = driver.run(AddOrganization("", postalCodes, carriers))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Reject if postal codes list is empty" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val carriers = List(Carrier("name", 18, Some(true)))

      val outcome = driver.run(AddOrganization("name", List(), carriers))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "Reject if carriers list is empty" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val postalCodes = List("59000", "76000")

      val outcome = driver.run(AddOrganization("name", postalCodes, List()))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }
  }

  "GetCarriers" should {
    "Return an empty list if nothing set" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")

      val outcome = driver.run(GetCarriers)

      outcome.events should ===(List())
      outcome.state.validated should ===(false)
      outcome.state.carriers should ===(List())
      outcome.replies should ===(List(Carriers(List())))
      outcome.issues should be(Nil)
    }

    "Return the carriers if set" in {
      val driver = new PersistentEntityTestDriver(system, new OrganizationEntity, "organization-1")
      val carrierId = "id"

      driver.run(AddCarrier(carrierId))
      val outcome = driver.run(GetCarriers)

      outcome.events should ===(List())
      outcome.state.validated should ===(true)
      outcome.state.carriers should ===(List(carrierId))
      outcome.replies should ===(List(Carriers(List(carrierId))))
      outcome.issues should be(Nil)
    }
  }
}
