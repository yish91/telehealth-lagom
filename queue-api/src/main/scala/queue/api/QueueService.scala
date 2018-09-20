package queue.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import queue.api.model.{QueueUpdateDto, WaitListEntryResultSummaryDto}
import play.api.libs.json.{Format, Json}

object QueueService  {
}

/**
  * The Queue service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the QueueApiService.
  */
trait QueueService extends Service {

  def getWaitListEntries(patientId: String, date: String): ServiceCall[NotUsed, WaitListEntryResultSummaryDto]

  def updateQueue(): ServiceCall[QueueUpdateDto, Done]

  /*def getAllQueues(): ServiceCall[NotUsed, QueueResultDto]

  def initiateQueue(): ServiceCall[QueueStateDto, Done]*/
  
  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("queue")
      .withCalls(
        restCall(Method.GET, "/api/queue/:patientId/:date", getWaitListEntries _),
        restCall(Method.POST, "/api/queue/update", updateQueue _),
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api/queue/.*")
      )
    // @formatter:on
  }
}