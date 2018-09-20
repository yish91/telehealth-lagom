package patient.impl

import akka.{Done, NotUsed}
import patient.api
import patient.api.{PatientService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import patient.api.model.{PatientResultDto, PatientStateDto}

import scala.concurrent.ExecutionContext

/**
  * Copyright FSS. 2018. All rights reserved.
  * Implementation of the KnolService.
  */
class PatientServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                        patientRepository: PatientRepository)(implicit ec: ExecutionContext) extends PatientService {


  override def getPatient(id: String): ServiceCall[NotUsed, PatientStateDto] = ServiceCall { _ =>
    // Look up the patient entity for the given ID.
    val ref = persistentEntityRegistry.refFor[PatientEntity](id)

    for {
      patientState <- ref.ask(GetPatientStateCmd)
    } yield {
      val patient = patientState.patient.getOrElse(throw new Exception("Patient does not exist!"))
      PatientStateDto(patient.id, patient.name, patient.gender, patient.race, patient.birthDate, patient.nationalId, patient.drugAllergies, patient.chronicIllnesses, patient.generalExclusions)
    }
  }

  override def initiatePatient(): ServiceCall[PatientStateDto, Done] = ServiceCall { req =>
    val ref = persistentEntityRegistry.refFor[PatientEntity](req.id)

    for {
      _ <- ref.ask(InitiatePatientCmd(req.id, req.name, req.gender, req.race, req.birthDate, req.nationalId, req.drugAllergies, req.chronicIllnesses, req.generalExclusions))
    } yield {
      Done
    }
  }

  override def getAllPatients(): ServiceCall[NotUsed, PatientResultDto] = ServiceCall { _ =>
    for {
      result <- patientRepository.getAllPatients()
    } yield {
      val patientStateDtoSeq = result.map(patient => PatientStateDto(patient.id, patient.name, patient.gender, patient.race, patient.birthDate, patient.nationalId, patient.drugAllergies, patient.chronicIllnesses, patient.generalExclusions))
      PatientResultDto(patientStateDtoSeq)
    }
  }
}
