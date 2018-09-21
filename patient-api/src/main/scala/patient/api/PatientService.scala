package patient.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import patient.api.model.{PatientResultDto, PatientStateDto}
import play.api.libs.json.{Format, Json}

object PatientService  {
}

/**
  * The Patient service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the PatientApiService.
  */
trait PatientService extends Service {

  def getPatient(id: String): ServiceCall[NotUsed, PatientStateDto]

  def getAllPatients(): ServiceCall[NotUsed, PatientResultDto]

  def initiatePatient(): ServiceCall[PatientStateDto, Done]
  
  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("patient")
      .withCalls(
        restCall(Method.GET, "/api/patient/search/all", getAllPatients _),
        restCall(Method.GET, "/api/patient/search/id/:id", getPatient _),
        restCall(Method.POST, "/api/patient/initiate", initiatePatient _)
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api/patient/.*")
      )
    // @formatter:on
  }
}
