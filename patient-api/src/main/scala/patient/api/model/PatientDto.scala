package patient.api.model

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class PatientStateDto(id: String,
                           name: String,
                           gender: String,
                           race: String,
                           birthDate: LocalDate,
                           nationalId: String,
                           drugAllergies: List[String],
                           chronicIllnesses: List[String],
                           generalExclusions: List[String])

object PatientStateDto {
  implicit val format: Format[PatientStateDto] = Json.format
}

case class PatientResultDto(result: Seq[PatientStateDto])

object PatientResultDto {
  implicit val format: Format[PatientResultDto] = Json.format
}