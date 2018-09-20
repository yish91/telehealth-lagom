package patient.impl

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import scala.concurrent.Promise

class PatientDbEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext) extends ReadSideProcessor[PatientEvent] {
  val patientRepository = new PatientRepository(session)

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[PatientEvent] = {
    readSide.builder[PatientEvent]("patientEventOffset")
      .setGlobalPrepare(patientRepository.createTable)
      .setPrepare(_ => patientRepository.createPreparedStatements)
      .setEventHandler[PatientInitiatedEvent](e => patientRepository.storePatient(e.event.patient))
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[PatientEvent]] = {
    Set(PatientEvent.Tag)
  }

}