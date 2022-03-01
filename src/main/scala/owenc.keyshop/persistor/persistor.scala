package owenc.keyshop.persistor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.io.Source.fromFile
import collection.immutable.List
import java.security.MessageDigest
import java.util.Base64
import java.io.{File, BufferedWriter, FileWriter}
import java.io.File
import java.nio.file.{Paths, Files}

object Persistor {
  def apply(dbLoc: String): Behavior[Command] =
    Behaviors.setup(context => {
      val dbPath = Paths.get(dbLoc)
      if (!Files.exists(dbPath)) {
        val dir = new File(dbLoc)
        if (!dir.mkdirs()) {
          println(s"Error, could not create ${dir}")
        }
      }
      new Persistor(context, dbLoc)
    })
  sealed trait Command
  final case class Persist(key: String, value: String) extends Command
  val keyList = "keyList.keyshop"
  def readExistingKeys = { (dbLoc: String, keyList: String) =>
    // expecting folder structure of:
    // dbLoc/keyList - is a file with a list of the keys that exist (new line separated)
    // dbLoc/md5(key) - is a file with the contents of the latest value of the key
    fromFile(s"${dbLoc}/${keyList}").getLines.map { key =>
      val fileName = getFileName(key)
      val value = fromFile(s"${dbLoc}/${fileName}").mkString
      (key, value)
    }
  }
  def getFileName = { (key: String) =>
    new String(
      Base64
        .getEncoder()
        .encode(
          MessageDigest
            .getInstance("md5")
            .digest((key).getBytes("UTF-8"))
        )
    )
  }
}

class Persistor(context: ActorContext[Persistor.Command], dbLoc: String)
    extends AbstractBehavior[Persistor.Command](context) {
  import Persistor._
  context.log.info("Persistor started")

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Persist(key, value) =>
        synchronized {
          val bw = new BufferedWriter(
            new FileWriter(new File(s"${dbLoc}/${getFileName(key)}"))
          )
          bw.write(value)
          bw.close()

          val bw2 = new BufferedWriter(
            new FileWriter(new File(s"${dbLoc}/${keyList}"), true)
          )
          bw2.write(key)
          bw2.write("\n")
          bw2.close()
        }
        this
    }
  }
}
