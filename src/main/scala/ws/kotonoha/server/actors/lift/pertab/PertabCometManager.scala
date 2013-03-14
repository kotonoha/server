package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.common.Box
import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import ws.kotonoha.server.actors.KotonohaMessage
import scala.concurrent.Await
import akka.pattern.ask
import concurrent.duration._
import ws.kotonoha.server.actors.lift.{PerUserMessage, PerUserActorSvc}
import akka.actor.Status.Failure
import net.liftweb.util.Helpers


/**
 * Maintain a Map[Value the actor monitors -> Ref to the Actor Dispatcher]
 *
 * For a url like: http://hostnbame/index/?p=icecream
 * If you name your actor based on the value of p
 * For each flavor that people have on their urls,
 * the map would be like:
 * chocolate -> code.comet.CometClassNames@ea5e9e7 ,
 * vanilla   -> code.comet.CometClassNames@wv9i7o3, etc
 *
 * If we have the actor already on the Map, just return it,
 * because it has to update the UI.
 * If wee do not have this actor on our Map. create a new
 * Dispatcher that will monitor this value, add it to our Map
 * and return the Ref to this new dispatcher so it updates the UI
 *
 *
 */

case class NamedComet(mytype: String, name: Box[String])

trait NamedCometMessage extends KotonohaMessage
case class LookupNamedComet(nc: NamedComet) extends NamedCometMessage
case class FreeNamedComet(nc: NamedComet) extends NamedCometMessage
case object Count


class PertabCometManager extends Actor with ActorLogging {

  private val listeners = new collection.mutable.HashMap[NamedComet, ActorRef]
  private lazy val perUser = context.actorOf(Props[PerUserActorSvc])

  def listenerFor(nc: NamedComet): Unit = {
    if (nc == null) {
      sender ! Failure(new NullPointerException("parameter nc is null"))
      return
    }
    val act = listeners.get(nc) match {
      case Some(a) => log.info("Our map is {}", listeners); a
      case None => {
        val ret = context.actorOf(Props(new CometDispatcher(nc, self)), nc.name.openOr(Helpers.nextFuncName))
        listeners += nc -> ret
        log.info("Our map is {}", listeners)
        ret
      }
    }
    sender ! act
  }

  override def receive = {
    case LookupNamedComet(nc) => listenerFor(nc)
    case FreeNamedComet(nc) => listeners.remove(nc).foreach(context.stop(_))
    case msg: PerUserMessage => perUser.forward(msg)
  }
}
