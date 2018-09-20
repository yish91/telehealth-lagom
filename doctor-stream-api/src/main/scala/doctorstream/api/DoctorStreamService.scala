package doctorstream.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

/**
  * Copyright FSS. 2018. All rights reserved.
  * The Doctor stream interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the DoctorStream service.
  */
trait DoctorStreamService extends Service {

  def stream: ServiceCall[Source[String, NotUsed], Source[String, NotUsed]]

  override final def descriptor: Descriptor = {
    import Service._

    named("doctor-stream")
    .withCalls(
      namedCall("stream", stream)
    ).withAutoAcl(true)
  }
}
