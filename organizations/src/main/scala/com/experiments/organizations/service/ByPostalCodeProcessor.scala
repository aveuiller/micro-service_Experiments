package com.experiments.organizations.service

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.experiments.organizations.api.models.GroupedOrganizations
import com.experiments.organizations.entities.{OrganizationAdded, OrganizationEvent}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future, Promise}

object ByPostalCodeProcessor {
  final val TABLE = "organizationsByPostalCodes"
  final val POSTAL_CODE = "postal_code"
  final val SIRET = "siret"

  /**
   * Ask for the SIRET of organizations grouped by postal code.
   *
   * @param session CassandraSession
   * @param ec      ExecutionContext
   * @param mat     Materializer
   * @return A [[Source]] of [[GroupedOrganizations]].
   */
  def groupedSelection(session: CassandraSession)
                      (implicit ec: ExecutionContext, mat: Materializer): Source[GroupedOrganizations, NotUsed] = {
    session.select(s"SELECT DISTINCT $POSTAL_CODE FROM $TABLE")
      .mapAsync[GroupedOrganizations](10) { row =>
      val postalCode = row.getString(POSTAL_CODE)
      retrieveIds(session, postalCode).map(ids => GroupedOrganizations(postalCode, ids))
    }
  }

  /**
   * Retrieve every SIRET of organizations present in the given postalCode.
   *
   * @param session    CassandraSession
   * @param postalCode The postal code to retrieve ids for.
   * @param ec         ExecutionContext
   * @param mat        Materializer
   * @return
   */
  private def retrieveIds(session: CassandraSession, postalCode: String)
                         (implicit ec: ExecutionContext, mat: Materializer): Future[List[String]] = {
    session.select(s"SELECT $SIRET FROM $TABLE WHERE $POSTAL_CODE = '$postalCode'")
      .runFold(List[String]()) { (list, row) => list :+ row.getString(SIRET) }
  }
}

class ByPostalCodeProcessor(session: CassandraSession, readSide: CassandraReadSide)
                           (implicit ec: ExecutionContext) extends ReadSideProcessor[OrganizationEvent] {


  private val writeTitlePromise = Promise[PreparedStatement] // initialized in prepare
  private def writeTitle: Future[PreparedStatement] = writeTitlePromise.future

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[OrganizationEvent] = {
    val builder = readSide.builder[OrganizationEvent]("organizationEventOffset")
    builder
      .setGlobalPrepare(prepareTable)
      .setPrepare(tag => prepareWrite())
      .setEventHandler[OrganizationAdded](processPostAdded)
      .build()
  }

  /**
   * Read side database table creation.
   *
   * @return Asynchronous method returning [[Done]].
   */
  private def prepareTable(): Future[Done] = {
    session.executeCreateTable(s"CREATE TABLE IF NOT EXISTS ${ByPostalCodeProcessor.TABLE} " +
      s"(${ByPostalCodeProcessor.POSTAL_CODE} TEXT, ${ByPostalCodeProcessor.SIRET} TEXT, " +
      s"PRIMARY KEY (${ByPostalCodeProcessor.POSTAL_CODE}, ${ByPostalCodeProcessor.SIRET}))")
  }

  /**
   * Prepare the insert statement.
   *
   * @return Asynchronous method returning [[Done]].
   */
  private def prepareWrite(): Future[Done] = {
    val f = session.prepare(s"INSERT INTO ${ByPostalCodeProcessor.TABLE} " +
      s"(${ByPostalCodeProcessor.POSTAL_CODE}, ${ByPostalCodeProcessor.SIRET}) VALUES (?, ?)")
    writeTitlePromise.completeWith(f)
    f.map(_ => Done)
  }

  /**
   * Process every event containing the tag [[OrganizationAdded]].
   *
   * @param eventElement The event to process.
   * @return The List of [[BoundStatement]].
   */
  private def processPostAdded(eventElement: EventStreamElement[OrganizationAdded]): Future[List[BoundStatement]] = {
    writeTitle.map { ps =>
      for (postalCode <- eventElement.event.postalCodes)
        yield bindStatement(ps, postalCode, eventElement.entityId)
    }
  }

  /**
   * Build the [[BoundStatement]] with the event data.
   *
   * @param ps         The [[PreparedStatement]] to use for generating a [[BoundStatement]].
   * @param postalCode The postal code to set in the statement.
   * @param siret      The SIRET code to set in the statement.
   * @return The created [[BoundStatement]].
   */
  private def bindStatement(ps: PreparedStatement, postalCode: String, siret: String): BoundStatement = {
    val bindWriteTitle = ps.bind()
    bindWriteTitle.setString(ByPostalCodeProcessor.POSTAL_CODE, postalCode)
    bindWriteTitle.setString(ByPostalCodeProcessor.SIRET, siret)
  }

  override def aggregateTags: Set[AggregateEventTag[OrganizationEvent]] =
    Set(OrganizationEvent.Tag)
}
