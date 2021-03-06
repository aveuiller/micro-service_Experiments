package com.experiments.organizations.service

import akka.{Done, NotUsed}
import com.experiments.carriers.api.events.CarrierCreated
import com.experiments.carriers.api.models
import com.experiments.carriers.api.models.{GroupedCarriers, Location}
import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.organizations.OrganizationsApplication
import com.experiments.organizations.api.models.{Carrier, Organization}
import com.experiments.organizations.api.service.OrganizationsServiceApi
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class OrganizationsServiceSpec extends AsyncWordSpec with Eventually with Matchers with ScalaFutures with BeforeAndAfterAll {
  var producerStub: ProducerStub[CarrierCreated] = _

  private def returnedCarrier(id: String): models.Carrier = {
    models.Carrier("name", 18, id, Some(true))
  }

  private class CarriersServiceStub(stub: ProducerStub[CarrierCreated]) extends CarriersServiceApi {
    override def create(): ServiceCall[models.Carrier, models.Carrier] = ServiceCall(Future.successful)

    override def fetch(id: String): ServiceCall[NotUsed, models.Carrier] = { _ =>
      Future.successful(returnedCarrier(id))
    }

    override def track(id: String): ServiceCall[Location, Done] = { _ => Future.successful(Done) }

    override def carrierCreatedTopic(): Topic[CarrierCreated] = stub.topic

    override def getLocation(id: String): ServiceCall[NotUsed, Location] = { _ => Future.successful(Location(1, 2, 3)) }

    override def groupByPostalCodes(): ServiceCall[NotUsed, List[GroupedCarriers]] = { _ =>
      Future.successful(List(GroupedCarriers("59000", List())))
    }
  }

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new OrganizationsApplication(ctx) with LocalServiceLocator {
      val stubFactory = new ProducerStubFactory(actorSystem, materializer)
      producerStub =
        stubFactory.producer[CarrierCreated](CarriersServiceApi.TOPIC_NAME)

      override lazy val carriersService = new CarriersServiceStub(producerStub)
    }
  }

  val client = server.serviceClient.implement[OrganizationsServiceApi]

  override protected def afterAll() = server.stop()


  private lazy val carrier = Carrier("name", 19, has_permis_a = Some(true))
  private lazy val organization = Organization("name", "siret", List("5900"), List(carrier))

  "create" should {
    "Reject creation if name is empty" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(Organization("", "siret", List("59000"), List(carrier))), 10.second)
      }
    }

    "Reject creation if siret is empty" in {
      val client = server.serviceClient.implement[OrganizationsServiceApi]
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(Organization("name", "", List("59000"), List(carrier))), 10.second)
      }
    }

    "Reject creation if postal_codes list is empty" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(Organization("name", "siret", List(), List(carrier))), 10.second)
      }
    }

    "Reject creation if carriers list is empty" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(Organization("name", "siret", List("5900"), List())), 10.second)
      }
    }

    "Accept creation even if carriers content is invalid" in {
      client.create().invoke(Organization("name", "siret", List("5900"), List(carrier.copy(name = "")))).map { result =>
        result should ===(Done)
      }
    }

    "Accept creation if everything is fine" in {
      client.create().invoke(organization).map { result =>
        result should ===(Done)
      }
    }
  }

  "carriers" should {
    "Return every carrier linked to the Organization" in {
      val id = "siret"
      client.create().invoke(organization) flatMap { _ =>
        producerStub.send(CarrierCreated("id", id))

        // Wait for event to be processed
        eventually(timeout(Span(5, Seconds))) {
          client.carriers(id).invoke().map { result =>
            result should ===(result
              .map(carr => Carrier(carr.name, carr.age, carr.has_permis_a, carr.has_permis_b, carr.has_permis_c))
            )
          }
        }
      }
    }
  }

  // TODO: See how to register a persistent ReadSide on tests
  //  "groupByPostalCodes" should {
  //    "Return the organization sirets grouped by postal code" in {
  //      val pcFirstOrga = "59000"
  //      val pcBothOrga = "27000"
  //      val organization1 = organization.copy(siret = "siret", postalCodes = List(pcFirstOrga, pcBothOrga))
  //      val organization2 = organization.copy(siret = "siret2", postalCodes = List(pcBothOrga))
  //
  //      client.create().invoke(organization1).flatMap { resultOrga =>
  //        client.create().invoke(organization2).flatMap { resultOrga2 =>
  //          // Wait for event to be processed
  //          eventually(timeout(Span(5, Seconds))) {
  //            client.groupByPostalCodes().invoke() map { grouped =>
  //              grouped.size should ===(2)
  //              grouped should contain(GroupedOrganizations(pcBothOrga, List(organization1.siret, organization2.siret)))
  //              grouped should contain(GroupedOrganizations(pcFirstOrga, List(organization1.siret)))
  //            }
  //          }
  //        }
  //      }
  //    }
  //  }
}
