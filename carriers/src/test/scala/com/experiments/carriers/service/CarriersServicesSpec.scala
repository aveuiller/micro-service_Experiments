package com.experiments.carriers.service

import akka.{Done, NotUsed}
import com.experiments.carriers.CarriersApplication
import com.experiments.carriers.api.models.{Carrier, GroupedCarriers, Location}
import com.experiments.carriers.api.service.CarriersServiceApi
import com.experiments.organizations.api
import com.experiments.organizations.api.models.{GroupedOrganizations, Organization}
import com.experiments.organizations.api.service.OrganizationsServiceApi
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CarriersServicesSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  private lazy val carrier = Carrier("name", 18, "siret", Some(true), Some(false), Some(false))

  private val carrierFirstOrga = api.models.Carrier("name", 18, id = Some("firstOrgaCarrier"))
  private val carrierSecondOrga = carrierFirstOrga.copy(id = Some("secondOrgaCarrier"))
  private val pcFirstOrga = "59000"
  private val pcBothOrga = "27000"
  private val idOrga1 = "siret"
  private val idOrga2 = "siret2"

  private class OrganizationsServiceStub() extends OrganizationsServiceApi {

    override def create(): ServiceCall[Organization, Done] = { _ => Future.successful(Done) }

    override def carriers(siret: String): ServiceCall[NotUsed, List[api.models.Carrier]] = { _ =>
      Future.successful(siret match {
        case `idOrga1` => List(carrierFirstOrga)
        case `idOrga2` => List(carrierSecondOrga)
      })
    }

    override def groupByPostalCodes(): ServiceCall[NotUsed, List[GroupedOrganizations]] = { _ =>
      Future.successful(List(
        GroupedOrganizations(pcFirstOrga, List(idOrga1)),
        GroupedOrganizations(pcBothOrga, List(idOrga1, idOrga2))
      ))
    }
  }


  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    // TODO : Fix Kafka test setup (java.lang.RuntimeException: Cannot provide the test topic factory as the default topic publisher since a default topic publisher has already been mixed into this cake: kafka)
    // See: https://www.lagomframework.com/documentation/1.3.x/scala/MessageBrokerTesting.html#Testing-publish
    new CarriersApplication(ctx) with LocalServiceLocator /*with TestTopicComponents*/ {
      override lazy val organizationsService = new OrganizationsServiceStub()
      /*override def optionalTopicFactory: Option[TopicFactory] = super[TestTopicComponents].optionalTopicFactory

      override def topicPublisherName: Option[String] = super[TestTopicComponents].topicPublisherName

      override lazy val topicFactory: TopicFactory = super[TestTopicComponents].optionalTopicFactory.get*/
    }
  }

  val client = server.serviceClient.implement[CarriersServiceApi]

  "create" should {
    "Reject creation if name is empty" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(carrier.copy(name = "")), 10.second)
      }
    }

    "Reject creation if age is <18" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(carrier.copy(age = 17)), 10.second)
      }
    }

    "Reject creation if the carrier has no license" in {
      assertThrows[BadRequest] {
        Await.result(client.create().invoke(carrier.copy(has_permis_a = None, has_permis_b = None, has_permis_c = None)), 10.second)
      }
    }

    "Accept to create a complete carrier" in {
      client.create().invoke(carrier).map { response =>
        response should ===(carrier.copy(id = response.id))
      }
    }
  }

  "fetch" should {
    "Return every carrier linked to the Organization with filled ID" in {
      val id = "carrierId"

      client.create().invoke(carrier).flatMap { response =>
        response.id should !==(None)
        client.fetch(response.id.get).invoke().map { result =>
          result should ===(carrier.copy(id = response.id))
        }
      }
    }
  }

  private val location = Location(1, 2, 3)
  "track" should {
    "Reject the tracking if the user is not valid" in {
      assertThrows[BadRequest] {
        Await.result(client.track("id").invoke(location), 10.second)
      }
    }

    "Create new tracking location if the user is valid" in {
      client.create().invoke(carrier).flatMap { carrier =>
        carrier.id should !==(None)
        client.track(carrier.id.get).invoke(location).map { response =>
          response should ===(Done)
        }
      }
    }

    "getLocation" should {
      "Retrieve the last tracking location" in {
        client.create().invoke(carrier).flatMap { carrier =>
          carrier.id should !==(None)
          client.track(carrier.id.get).invoke(location).flatMap { response =>
            response should ===(Done)
            client.getLocation(carrier.id.get).invoke().map { result =>
              result should ===(location)
            }
          }
        }
      }
    }
  }

  "groupByPostalCodes" should {
    "Return the carriers ID grouped by postal code" in {
      client.groupByPostalCodes().invoke() map { grouped =>
        grouped.size should ===(2)
        grouped should contain(GroupedCarriers(pcFirstOrga, List(carrierFirstOrga.id.get)))
        grouped should contain(GroupedCarriers(pcBothOrga, List(carrierFirstOrga.id.get, carrierSecondOrga.id.get)))
      }
    }
  }

  // TODO: Uncomment when kafka setup is correct.
  //  "carrierCreatedTopic" should {
  //    "Send a CarrierCreated event when a CarrierAdded is received" in {
  //      implicit val system = server.actorSystem
  //      implicit val mat = server.materializer
  //      val source = client.carrierCreatedTopic().subscribe.atMostOnceSource
  //
  //      client.create().invoke(carrier).flatMap { result =>
  //        source.runWith(TestSink.probe[CarrierCreated])
  //          .request(1)
  //          .expectNext should ===(CarrierCreated(result.id.get, carrier.organizationSiret))
  //
  //      }
  //    }
  //  }

}
