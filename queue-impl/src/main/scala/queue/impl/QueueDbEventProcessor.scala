package queue.impl

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

class QueueDbEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext) extends ReadSideProcessor[QueueEvent] {
  val queueRepository = new QueueRepository(session)

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[QueueEvent] = {
    readSide.builder[QueueEvent]("queueEventOffset")
      .setGlobalPrepare(queueRepository.createTable)
      .setPrepare(_ => queueRepository.createPreparedStatements)
      .setEventHandler[QueueUpdatedEvent](e => queueRepository.updateWaitListEntry(e.entityId, e.event.waitListEntry))
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[QueueEvent]] = {
    Set(QueueEvent.Tag)
  }

}