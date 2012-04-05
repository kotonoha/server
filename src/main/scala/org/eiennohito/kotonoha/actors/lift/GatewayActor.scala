/*
 * Copyright 2012 eiennohito
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

package org.eiennohito.kotonoha.actors.lift

import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import net.liftweb.common.SimpleActor

/**
 * @author eiennohito
 * @since 05.04.12
 */

class SimpleAkka(akka: ActorRef, lift: SimpleActor[Any]) extends SimpleActor[Any] {
  def !(msg: Any) = akka ! msg
}

case class RegisterLift(actor: SimpleActor[_])

class GatewayActor extends Actor {
  var liftActor: SimpleActor[_] = _
  protected def receive = {
    case RegisterLift(actor) => liftActor = actor
    case msg @ _ => liftActor ! msg
  }
}
