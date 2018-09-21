package queue.impl

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.Done
import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.typesafe.config.ConfigFactory
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

class QueueEntitySpec extends WordSpecLike with Matchers with BeforeAndAfterAll
  with TypeCheckedTripleEquals {

  val system = ActorSystem("QueueEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(QueueSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "Queue entity" must {
    "handle waiting" in {
      val driver = new PersistentEntityTestDriver(system, new QueueEntity, "queueEntity-1")
      val cmd = UpdateQueueCmd(patientId = "1", WaitListEntryStatus.WAITING)
      val outcome = driver.run(cmd)
      outcome.events.size shouldBe 1
      outcome.state.waitList.size shouldBe 1
      val wle = outcome.state.waitList.head
      wle.patientId shouldBe "1"
      wle.waitListEntryStates.head.status shouldBe WaitListEntryStatus.WAITING
    }

    "handle cancelled" in {
      val driver = new PersistentEntityTestDriver(system, new QueueEntity, "queueEntity-2")
      val cmd = UpdateQueueCmd(patientId = "1", WaitListEntryStatus.PATIENT_REJECTED)
      val outcome = driver.run(cmd)
      outcome.events.size shouldBe 0
      outcome.state.waitList.size shouldBe 0
    }

    "Multiple patient queue" in {
      val driver = new PersistentEntityTestDriver(system, new QueueEntity, "queueEntity-3")
      val cmd = UpdateQueueCmd(patientId = "1", WaitListEntryStatus.WAITING)
      val outcome = driver.run(cmd)
      outcome.events.size shouldBe 1
      outcome.state.waitList.size shouldBe 1
      val wle = outcome.state.waitList.head
      wle.patientId shouldBe "1"
      wle.waitListEntryStates.head.status shouldBe WaitListEntryStatus.WAITING
      val cmd2 = UpdateQueueCmd(patientId = "2", WaitListEntryStatus.WAITING)
      val outcome2 = driver.run(cmd2)
      outcome2.events.size shouldBe 1
      outcome2.state.waitList.size shouldBe 2
      outcome2.state.waitList.head shouldBe wle
      val wle2 = outcome2.state.waitList.last
      wle2.patientId shouldBe "2"
      wle2.waitListEntryStates.head.status shouldBe WaitListEntryStatus.WAITING

    }

  }
}
