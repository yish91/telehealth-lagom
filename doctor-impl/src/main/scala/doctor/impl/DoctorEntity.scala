package doctor.impl

import java.time.LocalDateTime

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

class DoctorEntity extends PersistentEntity {

  override type Command = DoctorCommand[_]
  override type Event = DoctorEvent
  override type State = DoctorState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: DoctorState = DoctorState(None)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case DoctorState(None) => handleInitiation.orElse(defaultActions)
    case _ => defaultActions
  }

  private def handleInitiation: Actions = {
    Actions().onCommand[InitiateDoctorCmd, Done] {
      case (InitiateDoctorCmd(id, name, gender, specialities), ctx, state) =>
        val doctor = Doctor(id, name, gender, specialities)
        ctx.thenPersist(DoctorInitiatedEvent(doctor))(_ => ctx.reply(Done))
    }.onEvent {
      case (evt: DoctorInitiatedEvent, state) => {
        state.updateDoctor(evt.doctor)
      }
    }
  }

  private def defaultActions: Actions = {
    Actions().onReadOnlyCommand[GetDoctorStateCmd.type, DoctorState] {
      case (GetDoctorStateCmd, ctx, state) =>
        ctx.reply(state)
    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class DoctorState(doctor: Option[Doctor]) {

  def updateDoctor(doctor: Doctor): DoctorState ={
    DoctorState(Some(doctor))
  }
}

object DoctorState {

  implicit val format: Format[DoctorState] = Json.format
}

case class Doctor(id: String,
                  name: String,
                  gender: String,
                  specialities: List[String])

object Doctor {

  implicit val format: Format[Doctor] = Json.format
}

/**
  * This interface defines all the events that the DoctorEntity supports.
  */
sealed trait DoctorEvent extends AggregateEvent[DoctorEvent] {
  def aggregateTag = DoctorEvent.Tag
}

object DoctorEvent {
  val Tag = AggregateEventTag[DoctorEvent]
}

/**
  * An event that represents a change in greeting message.
  */
case class GreetingMessageChanged(message: String) extends DoctorEvent

object GreetingMessageChanged {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[GreetingMessageChanged] = Json.format
}

case class DoctorInitiatedEvent(doctor: Doctor) extends DoctorEvent

object DoctorInitiatedEvent {

  implicit val format: Format[DoctorInitiatedEvent] = Json.format
}

/**
  * This interface defines all the commands that the HelloWorld entity supports.
  */
sealed trait DoctorCommand[R] extends ReplyType[R]

case object GetDoctorStateCmd extends DoctorCommand[DoctorState] {
}

case class InitiateDoctorCmd(id: String,
                             name: String,
                             gender: String,
                             specialities: List[String]) extends DoctorCommand[Done]

object InitiateDoctorCmd {
  implicit val format: Format[InitiateDoctorCmd] = Json.format
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
object DoctorSerializerRegistry extends JsonSerializerRegistry {
  import JsonSerializer.emptySingletonFormat

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[GreetingMessageChanged],
    JsonSerializer[DoctorInitiatedEvent],
    JsonSerializer(emptySingletonFormat(GetDoctorStateCmd)),
    JsonSerializer[InitiateDoctorCmd],
    JsonSerializer[Doctor],
    JsonSerializer[DoctorState]
  )
}
