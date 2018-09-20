package doctorstream.impl

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.ServiceCall
import doctorstream.api.DoctorStreamService
import doctor.api.DoctorService

import scala.concurrent.Future

/**
  * Copyright FSS. 2018. All rights reserved.
  * Implementation of the DoctorStreamService.
  */
class DoctorStreamServiceImpl(DoctorService: DoctorService) extends DoctorStreamService {
  def stream: ServiceCall[Source[String, NotUsed], Source[String, NotUsed]] = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(DoctorService.hello(_).invoke()))
  }
}
