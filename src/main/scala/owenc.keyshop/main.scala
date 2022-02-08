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

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import owenc.keyshop.keyshopSupervisor.KeyshopSupervisor
import owenc.keyshop.keyManager.KeyManager

object Main {
  implicit val resFormat = jsonFormat1(KeyshopSupervisor.RespondKey)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[KeyshopSupervisor.Command] =
      ActorSystem(KeyshopSupervisor(), "keyshop")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContext = system.executionContext

    val keyshop: ActorRef[KeyshopSupervisor.Command] = system

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
              complete(StatusCodes.Accepted, "Key updated")
            }
          }
        )
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}
