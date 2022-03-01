package owenc.keyshop.keyshopSupervisor
import akka.actor.typed.{Behavior, PostStop, Signal, ActorRef}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import owenc.keyshop.keyManager.KeyManager

object KeyshopSupervisor {
  def apply(): Behavior[Command] =
    Behaviors.setup[Command](context => new KeyshopSupervisor(context))
  sealed trait Command
  final case class GetKeys(replyTo: ActorRef[RespondKey]) extends Command
  final case class ReadKey(key: String, replyTo: ActorRef[RespondKey])
      extends Command
  final case class WriteKeyAsync(key: String, value: String) extends Command
  final case class WriteKey(
      key: String,
      value: String,
      replyTo: ActorRef[RespondKey]
  ) extends Command
  final case class RespondKey(key: String, value: Option[String])
      extends Command
}

class KeyshopSupervisor(context: ActorContext[KeyshopSupervisor.Command])
    extends AbstractBehavior[KeyshopSupervisor.Command](context) {
  import KeyshopSupervisor._
  context.log.info("Keyshop server started")
  private var keyManagers = Map.empty[String, ActorRef[KeyManager.Command]]

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case GetKeys(replyTo) => {
        keyManagers.toList.foreach { keyVal =>
          keyVal._2 ! KeyManager.ReadKey(replyTo)
        }
        this
      }
      case ReadKey(key, replyTo) => {
        keyManagers.get(key) match {
          case Some(actor) => {
            actor ! KeyManager.ReadKey(replyTo)
            this
          }
          case None => {
            val actor = context.spawn(KeyManager(key), s"key-manager-$key")
            keyManagers += key -> actor
            actor ! KeyManager.ReadKey(replyTo)
            this
          }
        }
      }
      case WriteKey(key, value, replyTo) => {
        keyManagers.get(key) match {
          case Some(actor) => {
            actor ! KeyManager.WriteKey(value, replyTo)
            this
          }
          case None => {
            val actor = context.spawn(KeyManager(key), s"key-manager-$key")
            keyManagers += key -> actor
            actor ! KeyManager.WriteKey(value, replyTo)
            this
          }
        }
      }
      case WriteKeyAsync(key, value) => {
        keyManagers.get(key) match {
          case Some(actor) => {
            actor ! KeyManager.WriteKeyAsync(value)
            this
          }
          case None => {
            val actor = context.spawn(KeyManager(key), s"key-manager-$key")
            keyManagers += key -> actor
            actor ! KeyManager.WriteKeyAsync(value)
            this
          }
        }
      }
      case _ => this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Keyshop server stopped")
      this
  }
}
