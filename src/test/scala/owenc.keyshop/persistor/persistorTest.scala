import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import java.io.File
import java.nio.file.{Paths, Files}
class PersistorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import owenc.keyshop.persistor._

  "Persistor actor" must {
    "save data to file" in {
      val tmpdir =
        Files.createTempDirectory("testingDir").toFile().getAbsolutePath()
      val persistor = spawn(Persistor(tmpdir))
      persistor ! Persistor.Persist("key", "value")
      Thread.sleep(200) // give time for write to happen
      Persistor.readExistingKeys(tmpdir, Persistor.keyList).foreach { res =>
        assert(res._1 == "key")
        assert(res._2 == "value")
      }
    }
  }

  "compress" must {
    "be able to be decompressed" in {
      val testStr = new String(
        Persistor.decompress(Persistor.compress("value".getBytes("UTF-8"))),
        "UTF-8"
      )
      assert(testStr == "value")
    }
  }
}
