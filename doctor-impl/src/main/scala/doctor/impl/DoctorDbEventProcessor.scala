package doctor.impl

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

class DoctorDbEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext) extends ReadSideProcessor[DoctorEvent] {
  val doctorRepository = new DoctorRepository(session)

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[DoctorEvent] = {
    readSide.builder[DoctorEvent]("doctorEventOffset")
      .setGlobalPrepare(doctorRepository.createTable)
      .setPrepare(_ => doctorRepository.createPreparedStatements)
      .setEventHandler[DoctorInitiatedEvent](e => doctorRepository.storeDoctor(e.event.doctor))
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[DoctorEvent]] = {
    Set(DoctorEvent.Tag)
  }

}