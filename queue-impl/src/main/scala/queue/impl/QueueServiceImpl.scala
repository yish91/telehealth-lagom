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
import queue.api.model._
import queue.impl.WaitListEntryStatus.WaitListEntryStatus

import scala.concurrent.{ExecutionContext, Future}

/**
  * Copyright FSS. 2018. All rights reserved.
  * Implementation of the KnolService.
  */
class QueueServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                        queueRepository: QueueRepository)(implicit ec: ExecutionContext) extends QueueService {


  override def getWaitListEntries(patientId: String, date: String): ServiceCall[NotUsed, WaitListEntryResultSummaryDto] = ServiceCall { _ =>
    // Look up the queue entity for the given ID.
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

  override def getDoctorQueue(doctorId: String): ServiceCall[NotUsed, DoctorQueueDto] = ServiceCall { _ =>
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = s"${doctorId}|${localDateStr}"
    val ref = persistentEntityRegistry.refFor[QueueEntity](id)
    println("HERE")
    for {
      result <- ref.ask(GetQueueStateCmd)
    } yield {
      val filterWaiting = result.waitList.filter(_.waitListEntryStates.maxBy(_.timestamp).status == WaitListEntryStatus.WAITING)
      println(filterWaiting)
      val patientIds = filterWaiting.sortBy(_.waitListEntryStates.maxBy(_.timestamp).timestamp).map(_.patientId)
      DoctorQueueDto(patientIds)
    }
  }

  override def updatePatientWait(): ServiceCall[QueueUpdateDto, Done] = ServiceCall { req =>
    val action = WaitListEntryStatus.WAITING
    updateQueueAction(req.patientId, req.doctorId, action)
  }

  override def updatePatientAccept(): ServiceCall[QueueUpdateDto, RoomResponseDto] = ServiceCall { req =>
    val action = WaitListEntryStatus.PATIENT_ACCEPTED
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = s"${req.doctorId}|${localDateStr}"
    val ref = persistentEntityRegistry.refFor[QueueEntity](id)
    for {
      result <- ref.ask(UpdateQueueCmd(req.patientId, action))
      waitListEntryResults <- if (result.action == ResultStatus.SUCCESS) queueRepository.getWaitListEntries(req.patientId, localDateStr) else Future.successful(Seq())
      filteredDoctorIds = waitListEntryResults.filter(w => w.doctorId != req.doctorId && w.waitListEntryStates.last.status == WaitListEntryStatus.WAITING).map(_.doctorId)
      _ <- Future.sequence(filteredDoctorIds.map(dId => {
        val tempId = s"${dId}|${localDateStr}"
        val tempRef = persistentEntityRegistry.refFor[QueueEntity](tempId)
        tempRef.ask(UpdateQueueCmd(req.patientId, WaitListEntryStatus.PATIENT_REJECTED))
      }))
    } yield {
      if (result.action == ResultStatus.SUCCESS){
        RoomResponseDto(generateHash(req.patientId, req.doctorId))
      } else {
        throw new Exception(result.message.getOrElse("ERROR"))
      }
    }
  }

  override def updatePatientReject(): ServiceCall[QueueUpdateDto, Done] = ServiceCall { req =>
    val action = WaitListEntryStatus.PATIENT_REJECTED
    updateQueueAction(req.patientId, req.doctorId, action)
  }

  override def updateDoctorAccept(): ServiceCall[QueueUpdateDto, RoomResponseDto] = ServiceCall { req =>
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = s"${req.doctorId}|${localDateStr}"
    val ref = persistentEntityRegistry.refFor[QueueEntity](id)
    for {
      result <- ref.ask(UpdateQueueCmd(req.patientId, WaitListEntryStatus.DOCTOR_ACCEPTED))
    } yield {
      if (result.action == ResultStatus.SUCCESS){
        RoomResponseDto(generateHash(req.patientId, req.doctorId))
      } else {
        throw new Exception(result.message.getOrElse("ERROR"))
      }
    }
  }

  override def updateDoctorReject(): ServiceCall[QueueUpdateDto, Done] = ServiceCall { req =>
    val action = WaitListEntryStatus.DOCTOR_REJECTED
    updateQueueAction(req.patientId, req.doctorId, action)
  }

  override def updateDoctorComplete(): ServiceCall[QueueUpdateDto, Done] = ServiceCall { req =>
    val action = WaitListEntryStatus.DOCTOR_COMPLETED
    updateQueueAction(req.patientId, req.doctorId, action)
  }

  def updateQueueAction(patientId: String, doctorId: String, action: WaitListEntryStatus): Future[Done.type] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = s"${doctorId}|${localDateStr}"
    val ref = persistentEntityRegistry.refFor[QueueEntity](id)
    for {
      result <- ref.ask(UpdateQueueCmd(patientId, action))
    } yield {
      if (result.action == ResultStatus.SUCCESS){
        Done
      } else {
        throw new Exception(result.message.getOrElse("ERROR"))
      }
    }
  }

  def generateHash(patientId: String, doctorId: String) = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDateStr = LocalDate.now().format(formatter)
    val id = s"${patientId}|${doctorId}|${localDateStr}"
    hashFn(id).toString
  }

  def hashFn(thiz: String): Int = {
    var res = 0
    var mul = 1 // holds pow(31, length-i-1)
    var i = thiz.length-1
    while (i >= 0) {
      res += thiz.charAt(i) * mul
      mul *= 31
      i -= 1
    }
    res & 0xfffffff
  }

}
