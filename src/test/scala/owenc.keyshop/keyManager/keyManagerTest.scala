import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class KeyManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import owenc.keyshop.keyManager._
  import owenc.keyshop.keyshopSupervisor.KeyshopSupervisor

  "KeyManager actor" must {

    "reply with empty value if no key is set" in {
      val probe = createTestProbe[KeyshopSupervisor.RespondKey]()
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.ReadKey(probe.ref)
      probe.expectMessage(
        KeyshopSupervisor.RespondKey(key = "key", value = None)
      )
    }

    "reply with the set value after being set" in {
      val writeProbe = createTestProbe[KeyshopSupervisor.RespondKey]()
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.WriteKey("value", writeProbe.ref)
      writeProbe.expectMessage(
        KeyshopSupervisor.RespondKey(key = "key", value = Some("value"))
      )

      val readProbe = createTestProbe[KeyshopSupervisor.RespondKey]()
      managerActor ! KeyManager.ReadKey(readProbe.ref)
      readProbe.expectMessage(
        KeyshopSupervisor.RespondKey(key = "key", value = Some("value"))
      )
    }

    "not fail when writing async" in {
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.WriteKeyAsync("value")
    }
  }
}
