package owenc.keyshop.persistor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.io.Source.fromFile
import collection.immutable.List
import java.security.MessageDigest
import java.util.Base64
import java.io.{ByteArrayOutputStream, File, BufferedWriter, FileWriter}
import java.nio.file.{Paths, Files}
import java.util.zip.{Deflater, Inflater}

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
    val keyListFile = s"${dbLoc}/${keyList}"
    if (!Files.exists(Paths.get(keyListFile))) {
      List.empty[(String, String)]
    } else {
      fromFile(keyListFile).getLines.toSet[String].map { key =>
        val fileName = getFileName(key)
        val value = fromFile(s"${dbLoc}/${fileName}").mkString
        (key, decompressB64(value))
      }
    }
  }

  def cleanupKeylistFile = { (dbLoc: String, keyList: String) =>
    val keyListFile = s"${dbLoc}/${keyList}"
    if (Files.exists(Paths.get(keyListFile))) {
      val existingKeys = fromFile(keyListFile).getLines.toSet[String]
      synchronized {
        val bw = new BufferedWriter(
          new FileWriter(new File(keyListFile))
        )
        existingKeys.foreach { key =>
          bw.write(s"${key}\n")
        }
        bw.close()
      }
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

  def compress = (bArray: Array[Byte]) => {
    val d = new Deflater(Deflater.BEST_COMPRESSION, true)
    d.setInput(bArray)
    d.finish()

    val outputStream = new ByteArrayOutputStream(bArray.length);
    val buffer = new Array[Byte](1024)
    while (!d.finished()) {
      val count = d.deflate(buffer)
      outputStream.write(buffer, 0, count)
    }
    outputStream.close
    d.end
    outputStream.toByteArray
  }

  def compressB64 = (str: String) => {
    new String(
      Base64
        .getEncoder()
        .encode(compress(str.getBytes("UTF-8"))),
      "UTF-8"
    )
  }

  def decompress = (bArray: Array[Byte]) => {
    val i = new Inflater(true)
    i.setInput(bArray)

    val outputStream = new ByteArrayOutputStream(bArray.length);
    val buffer = new Array[Byte](1024)
    while (!i.finished()) {
      val count = i.inflate(buffer)
      outputStream.write(buffer, 0, count)
    }
    outputStream.close
    i.end
    outputStream.toByteArray
  }

  def decompressB64 = (str: String) => {
    new String(
      decompress(
        Base64
          .getDecoder()
          .decode(str.getBytes("UTF-8"))
      ),
      "UTF-8"
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
          bw.write(compressB64(value))
          bw.close()

          val bw2 = new BufferedWriter(
            new FileWriter(new File(s"${dbLoc}/${keyList}"), true)
          )
          bw2.write(s"${key}\n")
          bw2.close()
        }
        this
    }
  }
}
