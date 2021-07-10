package owenc.keyshop.keyManager

import akka.actor.typed.{ActorRef}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.Behavior

object KeyManager {
  def apply(key: String): Behavior[Command] =
    Behaviors.setup(context => new KeyManager(context, key))
  sealed trait Command
  final case class ReadKey(replyTo: ActorRef[RespondKey]) extends Command
  final case class RespondKey(value: Option[String])
  final case class WriteKeyAsync(value: String) extends Command
  final case class WriteKey(value: String, replyTo: ActorRef[RespondWriteKey]) extends Command
  final case class RespondWriteKey(success: Boolean)
}

class KeyManager(context: ActorContext[KeyManager.Command], key: String)
  extends AbstractBehavior[KeyManager.Command](context) {
    import KeyManager._
    var keyVal: Option[String] = None
    context.log.info("KeyManager actor for key: {} started", key)

    override def onMessage(msg: Command): Behavior[Command] = {
      msg match {
        case ReadKey(replyTo) =>
          replyTo ! RespondKey(keyVal)
          this
        case WriteKeyAsync(value) =>
          keyVal = Some(value)
          this
        case WriteKey(value, replyTo) =>
          keyVal = Some(value)
          replyTo ! RespondWriteKey(true)
          this
      }
    }
}
