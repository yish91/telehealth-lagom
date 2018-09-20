package doctor.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import doctor.api.model.{DoctorResultDto, DoctorSpecialitiesDto, DoctorStateDto}
import play.api.libs.json.{Format, Json}

object DoctorService  {
  val TOPIC_NAME = "greetings"
}

/**
  * The doctor service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the DoctorApiService.
  */
trait DoctorService extends Service {

  def getDoctor(id: String): ServiceCall[NotUsed, DoctorStateDto]

  def getAllDoctors(): ServiceCall[NotUsed, DoctorResultDto]

  def searchDoctors(searchTerm: String): ServiceCall[NotUsed, DoctorResultDto]

  def getDoctorsWithSpecialities(): ServiceCall[DoctorSpecialitiesDto, DoctorResultDto]

  def initiateDoctor(): ServiceCall[DoctorStateDto, Done]

  /**
    * This gets published to Kafka.
    */
  def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("doctor")
      .withCalls(
        restCall(Method.GET, "/api/doctor/all", getAllDoctors _),
        restCall(Method.GET, "/api/doctor/:id", getDoctor _),
        restCall(Method.GET, "/api/doctor/search/:searchTerm", searchDoctors _),
        restCall(Method.POST, "/api/doctor/specialities", getDoctorsWithSpecialities _),
        restCall(Method.POST, "/api/doctor/initiate", initiateDoctor _)
      )
      .withTopics(
        topic(DoctorService.TOPIC_NAME, greetingsTopic)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[GreetingMessageChanged](_.name)
        )
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api/doctor/.*")
      )
    // @formatter:on
  }
}

/**
  * The greeting message class.
  */
case class GreetingMessage(message: String)

object GreetingMessage {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessage] = Json.format[GreetingMessage]
}



/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class GreetingMessageChanged(name: String, message: String)

object GreetingMessageChanged {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}
