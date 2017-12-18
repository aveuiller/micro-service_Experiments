package com.experiments.carriers.api.service

import akka.{Done, NotUsed}
import com.experiments.carriers.api.events.CarrierCreated
import com.experiments.carriers.api.models.{Carrier, GroupedCarriers, Location}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

object CarriersServiceApi {
  val TOPIC_NAME = "carrier-created"
}

trait CarriersServiceApi extends Service {

  def create(): ServiceCall[Carrier, Carrier]

  def fetch(id: String): ServiceCall[NotUsed, Carrier]

  def track(id: String): ServiceCall[Location, Done]

  def getLocation(id: String): ServiceCall[NotUsed, Location]

  def groupByPostalCodes(): ServiceCall[NotUsed, List[GroupedCarriers]]

  /**
   * This gets published to Kafka.
   */
  def carrierCreatedTopic(): Topic[CarrierCreated]

  override final def descriptor = {
    import com.lightbend.lagom.scaladsl.api.Service._
    // @formatter:off
    named("carriers")
      .withCalls(
        restCall(Method.POST, "/carriers", create _),
        pathCall("/carriers/by_postal_codes", groupByPostalCodes _),
        pathCall("/carriers/:id", fetch _),
        // Tracking
        restCall(Method.PUT, "/carriers/:id/tracking", track _),
        pathCall("/carriers/:id/tracking", getLocation _)
      )
      .withTopics(
        topic(CarriersServiceApi.TOPIC_NAME, carrierCreatedTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[CarrierCreated](_.organizationSiret)
        )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}





