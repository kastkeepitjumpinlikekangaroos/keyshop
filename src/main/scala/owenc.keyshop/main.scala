package owenc.keyshop.main

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._

import java.nio.file.{Paths, Files}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import owenc.keyshop.keyshopSupervisor.KeyshopSupervisor
import owenc.keyshop.keyManager.KeyManager
import owenc.keyshop.persistor.Persistor

object Main {
  implicit val resFormat = jsonFormat2(KeyshopSupervisor.RespondKey)

  def main(args: Array[String]): Unit = {
    val dbLoc = if (args.length > 1) args(0) else "./db"

    val persistor: ActorSystem[Persistor.Command] =
      ActorSystem(Persistor(dbLoc), "persistor")

    implicit val system: ActorSystem[KeyshopSupervisor.Command] =
      ActorSystem(KeyshopSupervisor(), "keyshop")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContext = system.executionContext

    val keyshop: ActorRef[KeyshopSupervisor.Command] = system

    val existingKeys = Persistor.readExistingKeys(dbLoc, Persistor.keyList)
    existingKeys.foreach { case (key, value) =>
      keyshop ! KeyshopSupervisor.WriteKeyAsync(key, value)
    }

    val route =
      pathPrefix("keyshop") {
        concat(
          get {
            path(Segment) { key =>
              implicit val timeout: Timeout = 5.seconds
              val res: Future[KeyshopSupervisor.RespondKey] =
                (keyshop ? (a => KeyshopSupervisor.ReadKey(key, a)))
                  .mapTo[KeyshopSupervisor.RespondKey]
              complete(res)
            }
          },
          put {
            path(Segment / Segment) { (key, value) =>
              keyshop ! KeyshopSupervisor.WriteKeyAsync(key, value)
              persistor ! Persistor.Persist(key, value)
              complete(KeyshopSupervisor.RespondKey(key, Some(value)))
            }
          },
          put {
            path(Segment / Segment / "sync") { (key, value) =>
              implicit val timeout: Timeout = 5.seconds
              persistor ! Persistor.Persist(key, value)
              val res: Future[KeyshopSupervisor.RespondKey] =
                (keyshop ? (a => KeyshopSupervisor.WriteKey(key, value, a)))
                  .mapTo[KeyshopSupervisor.RespondKey]
              complete(res)
            }
          }
        )
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    Persistor.cleanupKeylistFile(dbLoc, Persistor.keyList)
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => {
        system.terminate()
        persistor.terminate()
      }) // and shutdown when done
  }
}
