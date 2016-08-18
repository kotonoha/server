/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.kotonoha.server.actors.lift.pertab

import net.liftweb.common.Box
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ws.kotonoha.server.actors.KotonohaMessage

import scala.concurrent.Await
import akka.pattern.ask

import concurrent.duration._
import ws.kotonoha.server.actors.lift.{PerUserActorSvc, PerUserMessage}
import akka.actor.Status.Failure
import com.typesafe.scalalogging.StrictLogging
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


class PertabCometManager extends Actor with StrictLogging {

  private val listeners = new collection.mutable.HashMap[NamedComet, ActorRef]
  private lazy val perUser = context.actorOf(Props[PerUserActorSvc])

  def listenerFor(nc: NamedComet): Unit = {
    if (nc == null) {
      sender ! Failure(new NullPointerException("parameter nc is null"))
      return
    }
    val act = listeners.get(nc) match {
      case Some(a) => logger.trace("Our map is {}", listeners); a
      case None => {
        val ret = context.actorOf(Props(new CometDispatcher(nc, self)), nc.name.openOr(Helpers.nextFuncName))
        listeners += nc -> ret
        logger.trace("Our map is {}", listeners)
        ret
      }
    }
    sender ! act
  }

  override def receive = {
    case LookupNamedComet(nc) => listenerFor(nc)
    case FreeNamedComet(nc) => listeners.remove(nc).foreach(context.stop)
    case msg: PerUserMessage => perUser.forward(msg)
  }
}
