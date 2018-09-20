package queue.impl

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

class QueueRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  var queueStatement: PreparedStatement = _

  def createTable(): Future[Done] = {
    session.executeCreateTable(
      """
        |CREATE TABLE IF NOT EXISTS queue.waitListEntry(
        |patientId text,
        |date date,
        |timestamp bigint,
        |doctorId text,
        |status text,
        |primary key (patientId, date, timestamp)
        |);
      """.stripMargin)
  }

  def createPreparedStatements: Future[Done] = {
    for {
      queuePreparedStatement <- session.prepare("INSERT INTO queue.waitListEntry(patientId, date, timestamp, doctorId, status) VALUES (?, ?, ?, ?, ?)")
    } yield {
      queueStatement = queuePreparedStatement
      Done
    }
  }

  def updateWaitListEntry(entityId: String, waitListEntry: WaitListEntry) : Future[List[BoundStatement]] = {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val doctorId = if (entityId.contains('|')) entityId.split('|').head else ""
    val date = if (entityId.contains('|')) LocalDate.parse(entityId.split('|').last, dateFormat) else LocalDate.parse(entityId, dateFormat)

    val queueBindStatement = queueStatement.bind()
    queueBindStatement.setString("patientId", waitListEntry.patientId)
    queueBindStatement.setDate("date", com.datastax.driver.core.LocalDate.fromDaysSinceEpoch(date.toEpochDay.toInt))
    queueBindStatement.setLong("timestamp", waitListEntry.waitListEntryStates.head.timestamp.toEpochMilli)
    queueBindStatement.setString("doctorId", doctorId)
    queueBindStatement.setString("status", waitListEntry.waitListEntryStates.head.status.toString)
    Future.successful(List(queueBindStatement))
  }

  def getWaitListEntries(patientId: String, date: LocalDate): Future[Seq[WaitListEntryResult]] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateStr = date.format(formatter)
    for {
      waitListEntryResultList <- session.selectAll(s"SELECT * FROM queue.waitListEntry WHERE patientId = '$patientId' AND date = '$formatter' ALLOW FILTERING").map { optRow =>
        optRow.map { row =>
          val patientId = row.getString("patientId")
          val date = LocalDate.ofEpochDay(row.getDate("date").getDaysSinceEpoch)
          val timestamp = Instant.ofEpochMilli(row.getLong("timestamp"))
          val doctorId = row.getString("doctorId")
          val status = WaitListEntryStatus.withName(row.getString("status"))
          val wles = WaitListEntryState(status, timestamp)
          WaitListEntryResult(patientId, doctorId, date, List(wles))
        }
      }
    } yield {
      val groupedStatus = waitListEntryResultList.groupBy(_.doctorId)
      groupedStatus.map(s => {
        val wler = s._2
        val statuses: List[WaitListEntryState] = wler.flatMap(_.waitListEntryStates).toList
        WaitListEntryResult(patientId, s._1, date, statuses)
      }).toSeq
    }
  }

}
