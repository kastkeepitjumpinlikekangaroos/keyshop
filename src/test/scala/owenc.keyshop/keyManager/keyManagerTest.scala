import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class KeyManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import owenc.keyshop.keyManager

  "KeyManager actor" must {

    "reply with empty value if no key is set" in {
      val probe = createTestProbe[keyManager.KeyManager.RespondKey]()
      val managerActor = spawn(keyManager.KeyManager("key"))

      managerActor ! keyManager.KeyManager.ReadKey(probe.ref)
      val response = probe.receiveMessage()
      response.value should ===(None)
    }

    "reply with the set value after being set" in {
      val writeProbe = createTestProbe[keyManager.KeyManager.ResponseWriteKey]()
      val managerActor = spawn(keyManager.KeyManager("key"))

      managerActor ! keyManager.KeyManager.WriteKey("value", writeProbe.ref)
      val writeResponse = writeProbe.receiveMessage()
      writeResponse.success should ===(true)

      val readProbe = createTestProbe[keyManager.KeyManager.RespondKey]()
      managerActor ! keyManager.KeyManager.ReadKey(readProbe.ref)
      val readResponse = readProbe.receiveMessage()
      readResponse.value should ===(Some("value"))
    }

    "not fail when writing async" in {
      val managerActor = spawn(keyManager.KeyManager("key"))

      managerActor ! keyManager.KeyManager.WriteKeyAsync("value")
    }
  }
}
