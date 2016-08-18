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

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.http.BaseCometActor


/**
 * This class keeps a list of comet actors that need to update the UI
 */
class CometDispatcher(nm: NamedComet, listener: ActorRef) extends Actor with StrictLogging {

  def name = nm.name

  logger.info("DispatcherActor got name: {}", name)
  private var toUpdate = new collection.mutable.HashSet[BaseCometActor]


  override def receive = {
    /**
     * if we do not have this actor in the list, add it (register it)
     */
    case RegisterCometActor(actor, _) => {
      toUpdate += actor
    }

    case UnregisterCometActor(actor) => {
      logger.trace("before {}", toUpdate)
      toUpdate -= actor
      logger.trace("after {}", toUpdate)
      if (toUpdate.isEmpty) {
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
          logger.info("We will update this comet actor: {} showing name: {}", x, name)
        }
      }
    }
  }

}
