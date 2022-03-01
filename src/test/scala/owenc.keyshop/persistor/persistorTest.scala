import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import java.io.File
import java.nio.file.{Paths, Files}
class PersistorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import owenc.keyshop.persistor._

  "Persistor actor" must {
    "Save data to file" in {
      val tmpdir =
        Files.createTempDirectory("testingDir").toFile().getAbsolutePath()
      val persistor = spawn(Persistor(tmpdir))
      persistor ! Persistor.Persist("key", "value")
      Persistor.readExistingKeys(tmpdir, Persistor.keyList).foreach { res =>
        assert(res._1 == "key")
        assert(res._2 == "value")
      }
    }
  }
}
