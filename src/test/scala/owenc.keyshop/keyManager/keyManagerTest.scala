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
  }
}
