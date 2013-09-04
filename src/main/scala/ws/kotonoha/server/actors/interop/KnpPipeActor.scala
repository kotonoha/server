package ws.kotonoha.server.actors.interop

import akka.actor.Actor
import ws.kotonoha.server.japanese.parsing.Juman
import ws.kotonoha.akane.pipe.knp.KnpNode
import akka.actor.Status.Failure

/**
 * @author eiennohito
 * @since 2013-09-04
 */
trait KnpMessage extends JumanMessage
case class KnpRequest(s: String) extends KnpMessage
case class KnpResponse(surface: String, node: KnpNode)

class KnpException extends RuntimeException("error in knp")

class KnpPipeActor extends Actor {
  lazy val analyzer = Juman.knpExecutor(context.dispatcher)

  def receive = {
    case KnpRequest(s) =>
      val answer = analyzer.parse(s)
      answer match {
        case Some(n) => sender ! KnpResponse(s, n)
        case None => sender ! Failure(new KnpException)
      }
  }
}
