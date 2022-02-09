import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class KeyshopSupervisorSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import owenc.keyshop.keyshopSupervisor._
  import owenc.keyshop.keyManager._

  "KeyshopSupervisor actor" must {

    "reply with a KeyManager that can read and write its key value" in {
      val readProbe = createTestProbe[KeyshopSupervisor.RespondKey]()
      val supervisorActor = spawn(KeyshopSupervisor())

      supervisorActor ! KeyshopSupervisor.ReadKey("key", readProbe.ref)
      readProbe.expectMessage(KeyshopSupervisor.RespondKey(value = None))

      val successProbe = createTestProbe[KeyshopSupervisor.RespondKey]()
      supervisorActor ! KeyshopSupervisor.WriteKey(
        "key2",
        "value2",
        successProbe.ref
      )
      successProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value=Some("value2"))
      )

      supervisorActor ! KeyshopSupervisor.WriteKey(
        "key3",
        "value3",
        successProbe.ref
      )
      successProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value=Some("value3"))
      )

      supervisorActor ! KeyshopSupervisor.WriteKey(
        "key4",
        "value4",
        successProbe.ref
      )
      successProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value=Some("value4"))
      )

      supervisorActor ! KeyshopSupervisor.ReadKey("key2", readProbe.ref)
      readProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value = Some("value2"))
      )

      supervisorActor ! KeyshopSupervisor.ReadKey("key4", readProbe.ref)
      readProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value = Some("value4"))
      )

      supervisorActor ! KeyshopSupervisor.ReadKey("key3", readProbe.ref)
      readProbe.expectMessage(
        KeyshopSupervisor.RespondKey(value = Some("value3"))
      )

      supervisorActor ! KeyshopSupervisor.ReadKey("key", readProbe.ref)
      readProbe.expectMessage(KeyshopSupervisor.RespondKey(value = None))
    }
  }
}
