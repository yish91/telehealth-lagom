package queue.impl

import java.time.{Instant, LocalDate, LocalDateTime}

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json, Reads, Writes}
import queue.impl.WaitListEntryStatus.WaitListEntryStatus

import scala.collection.immutable.{HashMap, Seq}
import scala.concurrent.Future
import play.api.libs.json._
import queue.impl.ResultStatus.ResultStatus


class QueueEntity extends PersistentEntity {

  override type Command = QueueCommand[_]
  override type Event = QueueEvent
  override type State = QueueState

  val waitListEntryStatusMap = HashMap(
    WaitListEntryStatus.WAITING -> List(WaitListEntryStatus.ATTENDING, WaitListEntryStatus.CANCELLED, WaitListEntryStatus.REJECTED, WaitListEntryStatus.ONHOLD),
    WaitListEntryStatus.ATTENDING -> List(WaitListEntryStatus.COMPLETED, WaitListEntryStatus.CANCELLED, WaitListEntryStatus.REJECTED, WaitListEntryStatus.ONHOLD),
    WaitListEntryStatus.CANCELLED -> List(WaitListEntryStatus.WAITING),
    WaitListEntryStatus.REJECTED -> List(WaitListEntryStatus.WAITING, WaitListEntryStatus.COMPLETED),
    WaitListEntryStatus.ONHOLD -> List(WaitListEntryStatus.WAITING, WaitListEntryStatus.ATTENDING, WaitListEntryStatus.CANCELLED, WaitListEntryStatus.REJECTED),
    WaitListEntryStatus.COMPLETED -> List()
  )

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: QueueState = QueueState(id = entityId, waitList = List())

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case _ => handleActive.orElse(defaultActions)
  }

  private def handleActive: Actions = {
    Actions().onCommand[UpdateQueueCmd, QueueCmdResult] {
      case (UpdateQueueCmd(patientId, action), ctx, state) =>
        val latestStateOpt = state.waitList.find(_.patientId == patientId)
        val waitListEntryState = WaitListEntryState(status = action, timestamp = Instant.now)
        val waitListEntry = WaitListEntry(patientId = patientId, waitListEntryStates = List(waitListEntryState))
        if (latestStateOpt.isEmpty) {
          if (action == WaitListEntryStatus.WAITING) {
            ctx.thenPersist(QueueUpdatedEvent(waitListEntry))(_ => ctx.reply(QueueCmdResult(ResultStatus.SUCCESS, None)))
          } else {
            ctx.reply(QueueCmdResult(ResultStatus.FAILURE, Some(s"Unable to transit using action [${action.toString}]. No last status available")))
            ctx.done
          }
        } else {
          val latestState = latestStateOpt.get.waitListEntryStates.last
          val acceptableProgression = waitListEntryStatusMap(latestState.status).map(_.toString)
          if (acceptableProgression.contains(action.toString)) {
            ctx.thenPersist(QueueUpdatedEvent(waitListEntry))(_ => ctx.reply(QueueCmdResult(ResultStatus.SUCCESS, None)))
          } else {
            ctx.reply(QueueCmdResult(ResultStatus.FAILURE, Some(s"Unable to transit using action [${action.toString}]. Last status [${latestState.status.toString}]")))
            ctx.done
          }
        }

    }.onEvent {
      case (evt: QueueUpdatedEvent, state) => {
        state.updateWaitList(evt.waitListEntry, state)
      }
    }
  }

  private def defaultActions: Actions = {
    Actions().onReadOnlyCommand[GetQueueStateCmd.type, QueueState] {
      case (GetQueueStateCmd, ctx, state) =>
        ctx.reply(state)
    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class QueueState(id: String,
                      waitList: List[WaitListEntry]) {

  def updateWaitList(waitListEntry: WaitListEntry, state: QueueState): QueueState = {
    val waitListEntryOpt = state.waitList.find(_.patientId == waitListEntry.patientId)
    if (waitListEntryOpt.isDefined) {
      val filteredWaiListEntry = waitListEntryOpt.get
      val index = state.waitList.indexOf(filteredWaiListEntry)
      val filteredWaitListEntryStates = filteredWaiListEntry.waitListEntryStates
      val updatedWaitListEntryStates = filteredWaitListEntryStates.:::(waitListEntry.waitListEntryStates)
      val updatedFilteredWaitListEntry = filteredWaiListEntry.copy(waitListEntryStates = updatedWaitListEntryStates)
      val updatedWaitList = state.waitList.updated(index, updatedFilteredWaitListEntry)
      state.copy(waitList = updatedWaitList)
    } else {
      val updatedWaitList = state.waitList.:+(waitListEntry)
      state.copy(waitList = updatedWaitList)
    }
  }
}

object QueueState {

  implicit val format: Format[QueueState] = Json.format
}

case class WaitListEntry(patientId: String,
                         waitListEntryStates: List[WaitListEntryState])

object WaitListEntry {

  implicit val format: Format[WaitListEntry] = Json.format
}

case class WaitListEntryResult(patientId: String,
                               doctorId: String,
                               date: LocalDate,
                               waitListEntryStates: List[WaitListEntryState])

object WaitListEntryResult {

  implicit val format: Format[WaitListEntryResult] = Json.format
}

object WaitListEntryStatus extends Enumeration {
  type WaitListEntryStatus = Value
  val WAITING, ATTENDING, CANCELLED, REJECTED, COMPLETED, ONHOLD = Value

  implicit val waitListEntryStatusReads = Reads.enumNameReads(WaitListEntryStatus)
  implicit val waitListEntryStatusWrites = Writes.enumNameWrites
}

case class WaitListEntryState(status: WaitListEntryStatus,
                              timestamp: Instant)

object WaitListEntryState {

  implicit val format: Format[WaitListEntryState] = Json.format
}

/**
  * This interface defines all the events that the QueueEntity supports.
  */
sealed trait QueueEvent extends AggregateEvent[QueueEvent] {
  def aggregateTag = QueueEvent.Tag
}

object QueueEvent {
  val Tag = AggregateEventTag[QueueEvent]
}


case class QueueUpdatedEvent(waitListEntry: WaitListEntry) extends QueueEvent

object QueueUpdatedEvent {

  implicit val format: Format[QueueUpdatedEvent] = Json.format
}

/**
  * This interface defines all the commands that the HelloWorld entity supports.
  */
sealed trait QueueCommand[R] extends ReplyType[R]

case object GetQueueStateCmd extends QueueCommand[QueueState] {
}

case class UpdateQueueCmd(patientId: String,
                          action: WaitListEntryStatus) extends QueueCommand[QueueCmdResult]

object UpdateQueueCmd {
  implicit val format: Format[UpdateQueueCmd] = Json.format
}

object ResultStatus extends Enumeration {
  type ResultStatus = Value
  val SUCCESS, FAILURE = Value

  implicit val resultStatusReads = Reads.enumNameReads(ResultStatus)
  implicit val resultStatusWrites = Writes.enumNameWrites
}

case class QueueCmdResult(action: ResultStatus,
                          message: Option[String])

object QueueCmdResult {
  implicit val format: Format[QueueCmdResult] = Json.format
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
object QueueSerializerRegistry extends JsonSerializerRegistry {

  import JsonSerializer.emptySingletonFormat

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[QueueUpdatedEvent],
    JsonSerializer(emptySingletonFormat(GetQueueStateCmd)),
    JsonSerializer[UpdateQueueCmd],
    JsonSerializer[WaitListEntry],
    JsonSerializer[WaitListEntryState],
    JsonSerializer[WaitListEntryStatus],
    JsonSerializer[QueueCmdResult],
    JsonSerializer[ResultStatus],
    JsonSerializer[QueueState]
  )
}

/*object WaitListEntryStatus extends Enumeration {
  val WAITING = Value("WAITING")
  val ATTENDING = Value("ATTENDING")
  val CANCELLED = Value("CANCELLED")
  val REJECTED = Value("REJECTED")
  val COMPLETED = Value("COMPLETED")
  val ONHOLD = Value("ONHOLD")

  implicit val format = new Format[WaitListEntryStatus.Value] {
    override def writes(o: WaitListEntryStatus.Value): JsValue = Json.toJson(o.toString)
    override def reads(json: JsValue): JsResult[WaitListEntryStatus.Value] = json.validate[String].map(WaitListEntryStatus.withName(_))
  }
}*/
