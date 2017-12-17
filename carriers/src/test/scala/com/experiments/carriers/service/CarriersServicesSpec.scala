package com.experiments.carriers.service

import com.experiments.carriers.CarriersApplication
import com.experiments.carriers.api.models.Carrier
import com.experiments.carriers.api.service.CarriersServiceApi
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class CarriersServicesSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  private val carrier = Carrier("name", 18, "siret", Some(true), Some(false), Some(false))

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    // TODO : Fix Kafka test setup (java.lang.RuntimeException: Cannot provide the test topic factory as the default topic publisher since a default topic publisher has already been mixed into this cake: kafka)
    // See: https://www.lagomframework.com/documentation/1.3.x/scala/MessageBrokerTesting.html#Testing-publish
    new CarriersApplication(ctx) with LocalServiceLocator /*with TestTopicComponents {
      override def optionalTopicFactory: Option[TopicFactory] = super[TestTopicComponents].optionalTopicFactory

      override def topicPublisherName: Option[String] = super[TestTopicComponents].topicPublisherName

      override lazy val topicFactory: TopicFactory = super[TestTopicComponents].optionalTopicFactory.get
    }*/
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
    "Return every carrier linked to the Organization" in {
      val id = "carrierId"

      client.create().invoke(carrier).flatMap { response =>
        response.id should !==(None)
        client.fetch(response.id.get).invoke().map { result =>
          result should ===(carrier.copy(id = response.id))
        }
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
