package patient.impl

import java.time.LocalDate

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

class PatientRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  var patientStatement: PreparedStatement = _

  def createTable(): Future[Done] = {
    session.executeCreateTable(
      """
        |CREATE TABLE IF NOT EXISTS patient.patient(
        |id text PRIMARY KEY,
        |name text,
        |gender text,
        |race text,
        |birthDate date,
        |nationalId text,
        |drugAllergies text,
        |chronicIllnesses text,
        |generalExclusions text
        |);
      """.stripMargin)

    session.executeCreateTable(
      """
        |CREATE INDEX IF NOT EXISTS
        |name_index ON patient.patient (name);
      """.stripMargin)
  }

  def createPreparedStatements: Future[Done] = {
    for {
      patientPreparedStatement <- session.prepare("INSERT INTO patient.patient(id, name, gender, race, birthDate, nationalId, drugAllergies, chronicIllnesses, generalExclusions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
    } yield {
      patientStatement = patientPreparedStatement
      Done
    }
  }

  def storePatient(patient: Patient): Future[List[BoundStatement]] = {

    val patientBindStatement = patientStatement.bind()
    patientBindStatement.setString("id", patient.id)
    patientBindStatement.setString("name", patient.name)
    patientBindStatement.setString("gender", patient.gender)
    patientBindStatement.setString("race", patient.race)
    patientBindStatement.setDate("birthDate", com.datastax.driver.core.LocalDate.fromDaysSinceEpoch(patient.birthDate.toEpochDay.toInt))
    patientBindStatement.setString("nationalId", patient.nationalId)
    patientBindStatement.setString("drugAllergies", patient.drugAllergies.mkString(","))
    patientBindStatement.setString("chronicIllnesses", patient.chronicIllnesses.mkString(","))
    patientBindStatement.setString("generalExclusions", patient.generalExclusions.mkString(","))
    Future.successful(List(patientBindStatement))
  }

  def getPatientById(id: String): Future[Option[Patient]] =
    session.selectOne(s"SELECT * FROM patient.patient WHERE id = '$id'").map { optRow =>
      optRow.map { row =>
        val id = row.getString("id")
        val name = row.getString("name")
        val gender = row.getString("gender")
        val race = row.getString("race")
        val birthDate = LocalDate.from(row.getTimestamp("birthDate").toInstant)
        val nationalId = row.getString("nationalId")
        val drugAllergies = row.getString("drugAllergies").split(",").toList.map(_.trim())
        val chronicIllnesses = row.getString("chronicIllnesses").split(",").toList.map(_.trim())
        val generalExclusions = row.getString("generalExclusions").split(",").toList.map(_.trim())
        Patient(id, name, gender, race, birthDate, nationalId, drugAllergies, chronicIllnesses, generalExclusions)
      }
    }

  def getAllPatients(): Future[Seq[Patient]] =
    session.selectAll(s"SELECT * FROM patient.patient").map { optRow =>
      optRow.map { row =>
        val id = row.getString("id")
        val name = row.getString("name")
        val gender = row.getString("gender")
        val race = row.getString("race")
        val birthDate = LocalDate.ofEpochDay(row.getDate("birthDate").getDaysSinceEpoch)
        val nationalId = row.getString("nationalId")
        val drugAllergies = row.getString("drugAllergies").split(",").toList.map(_.trim())
        val chronicIllnesses = row.getString("chronicIllnesses").split(",").toList.map(_.trim())
        val generalExclusions = row.getString("generalExclusions").split(",").toList.map(_.trim())
        Patient(id, name, gender, race, birthDate, nationalId, drugAllergies, chronicIllnesses, generalExclusions)
      }
    }

}
