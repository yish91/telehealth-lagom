package patient.impl

import java.time.{LocalDate, LocalDateTime}

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

class PatientEntity extends PersistentEntity {

  override type Command = PatientCommand[_]
  override type Event = PatientEvent
  override type State = PatientState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: PatientState = PatientState(None)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case PatientState(None) => handleInitiation.orElse(defaultActions)
    case _ => defaultActions
  }

  private def handleInitiation: Actions = {
    Actions().onCommand[InitiatePatientCmd, Done] {
      case (InitiatePatientCmd(id, name, gender, race, birthDate, nationalId, drugAllergies, chronicIllnesses, generalExclusions), ctx, state) =>
        val patient = Patient(id, name, gender, race, birthDate, nationalId, drugAllergies, chronicIllnesses, generalExclusions)
        ctx.thenPersist(PatientInitiatedEvent(patient))(_ => ctx.reply(Done))
    }.onEvent {
      case (evt: PatientInitiatedEvent, state) => {
        state.updatePatient(evt.patient)
      }
    }
  }

  private def defaultActions: Actions = {
    Actions().onReadOnlyCommand[GetPatientStateCmd.type, PatientState] {
      case (GetPatientStateCmd, ctx, state) =>
        ctx.reply(state)
    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class PatientState(patient: Option[Patient]) {

  def updatePatient(patient: Patient): PatientState ={
    PatientState(Some(patient))
  }
}

object PatientState {

  implicit val format: Format[PatientState] = Json.format
}

case class Patient(id: String,
                   name: String,
                   gender: String,
                   race: String,
                   birthDate: LocalDate,
                   nationalId: String,
                   drugAllergies: List[String],
                   chronicIllnesses: List[String],
                   generalExclusions: List[String])

object Patient {

  implicit val format: Format[Patient] = Json.format
}

/**
  * This interface defines all the events that the PatientEntity supports.
  */
sealed trait PatientEvent extends AggregateEvent[PatientEvent] {
  def aggregateTag = PatientEvent.Tag
}

object PatientEvent {
  val Tag = AggregateEventTag[PatientEvent]
}


case class PatientInitiatedEvent(patient: Patient) extends PatientEvent

object PatientInitiatedEvent {

  implicit val format: Format[PatientInitiatedEvent] = Json.format
}

/**
  * This interface defines all the commands that the HelloWorld entity supports.
  */
sealed trait PatientCommand[R] extends ReplyType[R]

case object GetPatientStateCmd extends PatientCommand[PatientState] {
}

case class InitiatePatientCmd(id: String,
                              name: String,
                              gender: String,
                              race: String,
                              birthDate: LocalDate,
                              nationalId: String,
                              drugAllergies: List[String],
                              chronicIllnesses: List[String],
                              generalExclusions: List[String]) extends PatientCommand[Done]

object InitiatePatientCmd {
  implicit val format: Format[InitiatePatientCmd] = Json.format
}

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object PatientSerializerRegistry extends JsonSerializerRegistry {
  import JsonSerializer.emptySingletonFormat

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[PatientInitiatedEvent],
    JsonSerializer(emptySingletonFormat(GetPatientStateCmd)),
    JsonSerializer[InitiatePatientCmd],
    JsonSerializer[Patient],
    JsonSerializer[PatientState]
  )
}
