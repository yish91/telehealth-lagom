package queue.api.model

import java.time.{Instant, LocalDate}

import play.api.libs.json.{Format, Json}

case class QueueUpdateDto(patientId: String, doctorId: Option[String], action: String)

object QueueUpdateDto {

  implicit val format: Format[QueueUpdateDto] = Json.format
}

case class WaitListEntryResultSummaryDto(result: Seq[WaitListEntryResultDto])

object WaitListEntryResultSummaryDto {

  implicit val format: Format[WaitListEntryResultSummaryDto] = Json.format
}

case class WaitListEntryResultDto(patientId: String,
                                  doctorId: String,
                                  date: LocalDate,
                                  waitListEntryStates: Seq[WaitListEntryStateDto])

object WaitListEntryResultDto {

  implicit val format: Format[WaitListEntryResultDto] = Json.format
}

case class WaitListEntryStateDto(status: String,
                                 timestamp: Instant)

object WaitListEntryStateDto {

  implicit val format: Format[WaitListEntryStateDto] = Json.format
}