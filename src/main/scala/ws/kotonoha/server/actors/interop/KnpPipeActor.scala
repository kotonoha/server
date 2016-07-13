package ws.kotonoha.server.actors.interop

import akka.actor.Actor
import akka.actor.Status.Failure
import ws.kotonoha.akane.analyzers.knp.raw.KnpNode
import ws.kotonoha.server.KotonohaConfig

/**
 * @author eiennohito
 * @since 2013-09-04
 */
trait KnpMessage extends JumanMessage
case class KnpRequest(s: String) extends KnpMessage
case class KnpResponse(surface: String, node: KnpNode)

class KnpException extends RuntimeException("error in knp")

class KnpPipeActor extends Actor {
  lazy val analyzer = KotonohaConfig.knpExecutor(context.dispatcher)

  def receive = {
    case KnpRequest(s) =>
      if (s == "") {
        sender ! KnpResponse(s, new KnpNode(-15, "", Nil, Nil, Nil))
      } else {
        val answer = analyzer.parse(s)
        answer match {
          case Some(n) => sender ! KnpResponse(s, n)
          case None => sender ! Failure(new KnpException)
        }
      }
  }
}
