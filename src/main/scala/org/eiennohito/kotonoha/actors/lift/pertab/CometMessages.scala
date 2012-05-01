package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.http.CometActor
import net.liftweb.common.Box

/**
 * These are the message we pass around to
 * register each named comet actor with a dispatcher that
 * only updates the specific version it monitors
 */
case class RegisterCometActor(actor: CometActor, name: Box[String])
case class UnregisterCometActor(actor: CometActor)
case class CometName(name: String)
