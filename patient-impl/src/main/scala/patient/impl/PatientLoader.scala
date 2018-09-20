package patient.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._
import patient.api.PatientService
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

/**
  * Copyright FSS. 2018. All rights reserved.
  */
class PatientLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new PatientApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new PatientApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[PatientService])
}

abstract class PatientApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[PatientService](wire[PatientServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = PatientSerializerRegistry

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Register the Patient persistent entity
  persistentEntityRegistry.register(wire[PatientEntity])

  lazy val repository: PatientRepository = wire[PatientRepository]

  // Register the lagom persistent read side processor persistent entity
  readSide.register(wire[PatientDbEventProcessor])

}
