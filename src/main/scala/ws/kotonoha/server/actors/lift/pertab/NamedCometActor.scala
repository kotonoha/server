package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.common.{Box, Full, Logger}
import ws.kotonoha.server.actors.lift.AkkaInterop
import akka.actor.ActorRef
import akka.util.Timeout
import concurrent.duration._
import ws.kotonoha.server.util.DateTimeUtils._
import net.liftweb.http.CometActor
import concurrent.ExecutionContext
import net.liftweb.util.Helpers.TimeSpan


trait NamedCometActor extends CometActor with Logger with AkkaInterop {

  private def namedComet = NamedComet(this.getClass.getName, name)
  private implicit val timeout: Timeout = 1 second
  private implicit val ec: ExecutionContext = akkaServ.context

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
  override def lifespan: Box[TimeSpan] = Full(5 minutes)
}
