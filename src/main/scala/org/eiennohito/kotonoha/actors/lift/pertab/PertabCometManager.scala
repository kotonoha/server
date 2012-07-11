package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.actor.LiftActor
import net.liftweb.common.{Box, Logger}
import akka.actor.{Props, ActorRef, Actor}
import org.eiennohito.kotonoha.actors.KotonohaMessage
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import org.eiennohito.kotonoha.actors.lift.{PerUserMessage, PerUserActorSvc}


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


class PertabCometManager extends Actor with Logger {

  private val listeners = new collection.mutable.HashMap[NamedComet, ActorRef]
  private lazy val perUser = context.actorOf(Props[PerUserActorSvc])

  def listenerFor(nc: NamedComet): ActorRef = synchronized {
    listeners.get(nc) match {
      case Some(a) => info("Our map is %s".format(listeners)); a
      case None => {
        val ret = context.actorOf(Props(new CometDispatcher(nc, self)))
        listeners += nc -> ret
        info("Our map is %s".format(listeners))
        ret
      }
    }
  }

  protected def receive = {
    case LookupNamedComet(nc) => sender ! listenerFor(nc)
    case FreeNamedComet(nc) => {
      listeners.get(nc).map ( a => {
        val cnt = Await.result(ask(a, Count)(2 seconds).mapTo[Int], 2 seconds)
        if (cnt == 0) { listeners.remove(nc).map(context.stop(_)) }
      })
    }
    case msg: PerUserMessage => perUser.forward(msg)
  }
}
