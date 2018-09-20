package doctor.impl

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

class DoctorRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  var doctorStatement: PreparedStatement = _

  def createTable(): Future[Done] = {
    session.executeCreateTable(
      """
        |CREATE TABLE IF NOT EXISTS doctor.doctor(
        |id text PRIMARY KEY,
        |name text,
        |gender text,
        |specialities text
        |);
      """.stripMargin)

    session.executeCreateTable(
      """
        |CREATE INDEX IF NOT EXISTS
        |name_index ON doctor.doctor (name);
      """.stripMargin)
  }

  def createPreparedStatements: Future[Done] = {
    for {
      doctorPreparedStatement <- session.prepare("INSERT INTO doctor.doctor(id, name, gender, specialities) VALUES (?, ?, ?, ?)")
    } yield {
      doctorStatement = doctorPreparedStatement
      Done
    }
  }

  def storeDoctor(doctor: Doctor): Future[List[BoundStatement]] = {
    val doctorBindStatement = doctorStatement.bind()
    doctorBindStatement.setString("id", doctor.id)
    doctorBindStatement.setString("name", doctor.name)
    doctorBindStatement.setString("gender", doctor.gender)
    doctorBindStatement.setString("specialities", doctor.specialities.mkString(","))
    Future.successful(List(doctorBindStatement))
  }

  def getDoctorById(id: String): Future[Option[Doctor]] =
    session.selectOne(s"SELECT * FROM doctor.doctor WHERE id = '$id'").map { optRow =>
      optRow.map { row =>
        val id = row.getString("id")
        val name = row.getString("name")
        val gender = row.getString("gender")
        val specialities = row.getString("specialities").split(",").toList.map(_.trim())
        Doctor(id, name, gender, specialities)
      }
    }

  def getAllDoctors(): Future[Seq[Doctor]] =
    session.selectAll(s"SELECT * FROM doctor.doctor").map { optRow =>
      optRow.map { row =>
        val id = row.getString("id")
        val name = row.getString("name")
        val gender = row.getString("gender")
        val specialities = row.getString("specialities").split(",").toList.map(_.trim())
        Doctor(id, name, gender, specialities)
      }
    }

}
