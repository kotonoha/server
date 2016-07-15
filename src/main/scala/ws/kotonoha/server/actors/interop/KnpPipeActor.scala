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

package ws.kotonoha.server.actors.interop

import akka.actor.Actor
import akka.actor.Status.Failure
import ws.kotonoha.akane.analyzers.knp.raw.KnpNode
import ws.kotonoha.server.KotonohaConfig

/**
 * @author eiennohito
 * @since 2013-09-04
 */
trait KnpMessage extends JumanMessage
case class KnpRequest(s: String) extends KnpMessage
case class KnpResponse(surface: String, node: KnpNode)

class KnpException extends RuntimeException("error in knp")

class KnpPipeActor extends Actor {
  lazy val analyzer = KotonohaConfig.knpExecutor(context.dispatcher)

  def receive = {
    case KnpRequest(s) =>
      if (s == "") {
        sender ! KnpResponse(s, new KnpNode(-15, "", Nil, Nil, Nil))
      } else {
        val answer = analyzer.parse(s)
        answer match {
          case Some(n) => sender ! KnpResponse(s, n)
          case None => sender ! Failure(new KnpException)
        }
      }
  }
}
