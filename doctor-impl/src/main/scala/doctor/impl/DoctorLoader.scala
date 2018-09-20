package doctor.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._
import doctor.api.DoctorService
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

/**
  * Copyright FSS. 2018. All rights reserved.
  */
class DoctorLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new DoctorApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new DoctorApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[DoctorService])
}

abstract class DoctorApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[DoctorService](wire[DoctorServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = DoctorSerializerRegistry

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Register the Doctor persistent entity
  persistentEntityRegistry.register(wire[DoctorEntity])

  lazy val repository: DoctorRepository = wire[DoctorRepository]

  // Register the lagom persistent read side processor persistent entity
  readSide.register(wire[DoctorDbEventProcessor])

}
