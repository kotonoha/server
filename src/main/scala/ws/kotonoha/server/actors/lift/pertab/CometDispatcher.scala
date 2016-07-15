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

package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.http.CometActor
import net.liftweb.common.Full
import akka.actor.{PoisonPill, ActorLogging, ActorRef, Actor}


/**
 * This class keeps a list of comet actors that need to update the UI
 */
class CometDispatcher(nm: NamedComet, listener: ActorRef) extends Actor with ActorLogging {

  def name = nm.name

  log.info("DispatcherActor got name: {}", name)
  private var toUpdate = new collection.mutable.HashSet[CometActor]


  override def receive = {
    /**
     * if we do not have this actor in the list, add it (register it)
     */
    case RegisterCometActor(actor, _) => {
      toUpdate += actor
    }

    case UnregisterCometActor(actor) => {
      log.info("before {}", toUpdate)
      toUpdate -= actor
      log.info("after {}", toUpdate)
      if (toUpdate.size == 0) {
        listener ! FreeNamedComet(nm)
      }
    }

    //Catch the dummy message we send on comet creation
    case CometName(name) =>

    case Count => sender ! toUpdate.size

    /**
     * Go through the list of actors and send them a message
     */
    case msg => {
      toUpdate.foreach {
        x => {
          x ! msg
          log.info("We will update this comet actor: {} showing name: {}", x, name)
        }
      }
    }
  }

}
