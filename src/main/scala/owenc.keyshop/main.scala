package owenc.keyshop.main
import akka.actor.typed.{ActorSystem, ActorRef}
import owenc.keyshop.keyshopSupervisor.KeyshopSupervisor
import owenc.keyshop.keyManager.KeyManager


object Main {
    def main(args: Array[String]): Unit = {
      val supervisor = ActorSystem[KeyshopSupervisor.Command](KeyshopSupervisor(), "keyshop-supervisor")
    }
}
