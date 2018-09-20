package doctor.impl

import akka.{Done, NotUsed}
import doctor.api
import doctor.api.{DoctorService, GreetingMessage}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import doctor.api.model.{DoctorResultDto, DoctorSpecialitiesDto, DoctorStateDto}

import scala.concurrent.ExecutionContext

/**
  * Copyright FSS. 2018. All rights reserved.
  * Implementation of the KnolService.
  */
class DoctorServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                        doctorRepository: DoctorRepository)(implicit ec: ExecutionContext) extends DoctorService {


  override def getDoctor(id: String): ServiceCall[NotUsed, DoctorStateDto] = ServiceCall { _ =>
    // Look up the doctor entity for the given ID.
    val ref = persistentEntityRegistry.refFor[DoctorEntity](id)

    for {
      doctorState <- ref.ask(GetDoctorStateCmd)
    } yield {
      val doctor = doctorState.doctor.getOrElse(throw new Exception("Doctor does not exist!"))
      DoctorStateDto(doctor.id, doctor.name, doctor.gender, doctor.specialities)
    }
  }

  override def initiateDoctor(): ServiceCall[DoctorStateDto, Done] = ServiceCall { req =>
    val ref = persistentEntityRegistry.refFor[DoctorEntity](req.id)

    for {
      _ <- ref.ask(InitiateDoctorCmd(req.id, req.name, req.gender, req.specialities))
    } yield {
      Done
    }
  }

  override def getAllDoctors(): ServiceCall[NotUsed, DoctorResultDto] = ServiceCall { _ =>
    for {
      result <- doctorRepository.getAllDoctors()
    } yield {
      val doctorStateDtoSeq = result.map(doctor => DoctorStateDto(doctor.id, doctor.name, doctor.gender, doctor.specialities))
      DoctorResultDto(doctorStateDtoSeq)
    }
  }

  override def searchDoctors(searchTerm: String): ServiceCall[NotUsed, DoctorResultDto] = ServiceCall { _ =>
    for {
      resultSeq <- getAllDoctors.invoke()
    } yield {
      val result = resultSeq.result.toList
      val nameFilter = result.filter(_.name.toUpperCase.contains(searchTerm.toUpperCase))
      val specialityFilter = result.filter(_.specialities.exists(s => s.toUpperCase.contains(searchTerm.toUpperCase)))
      val combinedSearch = nameFilter.:::(specialityFilter).toSet.toSeq
      DoctorResultDto(combinedSearch)
    }
  }

  override def getDoctorsWithSpecialities(): ServiceCall[DoctorSpecialitiesDto, DoctorResultDto] = ServiceCall { req =>
    for {
      result <- getAllDoctors.invoke()
    } yield {
      val specialities = req.specialities.map(_.toUpperCase)
      val doctors = result.result.filter(_.specialities.exists(s => specialities.exists(speciality => s.toUpperCase.contains(speciality))))
      DoctorResultDto(doctors)
    }
  }



  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(DoctorEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[DoctorEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
      case _ => api.GreetingMessageChanged(helloEvent.entityId, "hi!")
    }
  }
}
