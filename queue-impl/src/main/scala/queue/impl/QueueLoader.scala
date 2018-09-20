package queue.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents
import queue.api.QueueService

/**
  * Copyright FSS. 2018. All rights reserved.
  */
class QueueLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new QueueApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new QueueApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[QueueService])
}

abstract class QueueApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[QueueService](wire[QueueServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = QueueSerializerRegistry

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Register the Queue persistent entity
  persistentEntityRegistry.register(wire[QueueEntity])

  lazy val repository: QueueRepository = wire[QueueRepository]

  // Register the lagom persistent read side processor persistent entity
  readSide.register(wire[QueueDbEventProcessor])

}
