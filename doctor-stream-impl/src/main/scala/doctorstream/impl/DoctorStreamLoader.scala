package doctorstream.impl

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import doctorstream.api.DoctorStreamService
import doctor.api.DoctorService
import com.softwaremill.macwire._

/**
  * Copyright FSS. 2018. All rights reserved.
  */
class DoctorStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new DoctorStreamApplication(context) {
    override def serviceLocator = NoServiceLocator
  }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new DoctorStreamApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[DoctorStreamService])
}

abstract class DoctorStreamApplication(context: LagomApplicationContext)
extends LagomApplication(context)
with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[DoctorStreamService](wire[DoctorStreamServiceImpl])

  // Bind the $name;format="Camel"$Service client
  lazy val doctor$Service = serviceClient.implement[DoctorService]
}
