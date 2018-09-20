package queue.impl

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.{Done, NotUsed}
import queue.api
import queue.api.QueueService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import queue.api.model.{QueueUpdateDto, WaitListEntryResultDto, WaitListEntryResultSummaryDto, WaitListEntryStateDto}
import queue.impl.WaitListEntryStatus.WaitListEntryStatus

import scala.concurrent.ExecutionContext

/**
  * Copyright FSS. 2018. All rights reserved.
  * Implementation of the KnolService.
  */
class QueueServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                        queueRepository: QueueRepository)(implicit ec: ExecutionContext) extends QueueService {


  override def getWaitListEntries(patientId: String, dateStr: String): ServiceCall[NotUsed, WaitListEntryResultSummaryDto] = ServiceCall { _ =>
    // Look up the queue entity for the given ID.
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val date = LocalDate.parse(dateStr, formatter)

    for {
      result <- queueRepository.getWaitListEntries(patientId, date)
    } yield {
      val waitListEntryResults = result.map(r => {
        val waitListEntryStates = r.waitListEntryStates.map(s => WaitListEntryStateDto(status = s.status.toString, timestamp = s.timestamp))
        WaitListEntryResultDto(patientId = r.patientId, doctorId = r.doctorId, date = r.date, waitListEntryStates = waitListEntryStates)
      })
      WaitListEntryResultSummaryDto(waitListEntryResults)
    }
  }

  override def updateQueue(): ServiceCall[QueueUpdateDto, Done] = ServiceCall { req =>
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = if (req.doctorId.isDefined) s"${req.doctorId.get}|${localDateStr}" else localDateStr
    val ref = persistentEntityRegistry.refFor[QueueEntity](id)
    val action = WaitListEntryStatus.withName(req.action)
    for {
      result <- ref.ask(UpdateQueueCmd(req.patientId, action))
    } yield {
      if (result.action == ResultStatus.SUCCESS){
        Done
      } else {
        throw new Exception(result.message.getOrElse("ERROR"))
      }
    }
  }
}
