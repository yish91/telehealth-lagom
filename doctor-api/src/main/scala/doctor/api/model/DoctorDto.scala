package doctor.api.model

import play.api.libs.json.{Format, Json}

case class DoctorStateDto(id: String,
                          name: String,
                          gender: String,
                          specialities: List[String])

object DoctorStateDto {
  implicit val format: Format[DoctorStateDto] = Json.format
}

case class DoctorSpecialitiesDto(specialities: List[String])

object DoctorSpecialitiesDto {
  implicit val format: Format[DoctorSpecialitiesDto] = Json.format
}

case class DoctorResultDto(result: Seq[DoctorStateDto])

object DoctorResultDto {
  implicit val format: Format[DoctorResultDto] = Json.format
}