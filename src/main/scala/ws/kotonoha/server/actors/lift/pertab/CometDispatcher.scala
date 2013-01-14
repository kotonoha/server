package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.http.CometActor
import net.liftweb.common.{Full, Logger}
import akka.actor.{ActorRef, Actor}


/**
 * This class keeps a list of comet actors that need to update the UI
 */
class CometDispatcher(nm: NamedComet, listener: ActorRef) extends Actor with Logger {

  def name = nm.name

  info("DispatcherActor got name: %s".format(name))
  private var toUpdate = new collection.mutable.HashSet[CometActor]


  override def receive = {
    /**
     * if we do not have this actor in the list, add it (register it)
     */
    case RegisterCometActor(actor, Full(name)) => {
      toUpdate += actor
    }

    case UnregisterCometActor(actor) => {
      info("before %s".format(toUpdate))
      toUpdate -= actor
      info("after %s".format(toUpdate))
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
          info("We will update this comet actor: %s showing name: %s".format(x, name))
        }
      }
    }
  }

}
