package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb._
import common.{Full, Logger}
import http.CometActor
import org.eiennohito.kotonoha.actors.lift.AkkaInterop
import akka.actor.ActorRef
import akka.util.Timeout
import akka.util.duration._
import org.eiennohito.kotonoha.util.DateTimeUtils._


trait NamedCometActor extends CometActor with Logger with AkkaInterop {

  private val namedComet = NamedComet(this.getClass.getName, name)
  private implicit val timeout: Timeout = 1 second

  /**
   * First thing we do is registering this comet actor
   * for the "name" key
   */
  override def localSetup = {
    (akkaServ ? LookupNamedComet(namedComet)).mapTo[ActorRef] map (_ ! RegisterCometActor(this, name))
    super.localSetup()
  }

  /**
   * We remove the comet from the map of registers actors
   */
  override def localShutdown = {
    (akkaServ ? LookupNamedComet(namedComet)).mapTo[ActorRef] map (_ ! UnregisterCometActor(this))
    super.localShutdown()
  }

  // time out the comet actor if it hasn't been on a page for 2 minutes
  override def lifespan = Full(2 minutes)
}
