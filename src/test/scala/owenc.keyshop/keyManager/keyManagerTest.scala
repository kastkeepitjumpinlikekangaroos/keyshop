import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class KeyManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import owenc.keyshop.keyManager._

  "KeyManager actor" must {

    "reply with empty value if no key is set" in {
      val probe = createTestProbe[KeyManager.RespondKey]()
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.ReadKey(probe.ref)
      probe.expectMessage(KeyManager.RespondKey(value=None))
    }

    "reply with the set value after being set" in {
      val writeProbe = createTestProbe[KeyManager.ResponseWriteKey]()
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.WriteKey("value", writeProbe.ref)
      writeProbe.expectMessage(KeyManager.ResponseWriteKey(success=true))

      val readProbe = createTestProbe[KeyManager.RespondKey]()
      managerActor ! KeyManager.ReadKey(readProbe.ref)
      readProbe.expectMessage(KeyManager.RespondKey(value=Some("value")))
    }

    "not fail when writing async" in {
      val managerActor = spawn(KeyManager("key"))

      managerActor ! KeyManager.WriteKeyAsync("value")
    }
  }
}
