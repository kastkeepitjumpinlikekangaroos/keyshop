package owenc.keyshop.main
import akka.actor.typed.{Behavior, PostStop, Signal, ActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


object Main {
    def main(args: Array[String]): Unit = {
      ActorSystem[Nothing](KeyshopSupervisor(), "keyshop-supervisor")
    }
}

object KeyshopSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new KeyshopSupervisor(context))
}

class KeyshopSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("Keyshop server started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal,Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("Keyshop server stopped")
      this
  }
}
