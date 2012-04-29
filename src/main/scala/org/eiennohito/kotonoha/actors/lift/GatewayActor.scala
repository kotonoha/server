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

import akka.pattern.{ask => apa}
import net.liftweb.http.CometActor
import org.eiennohito.kotonoha.actors.ioc.Akka
import org.eiennohito.kotonoha.actors.KotonohaMessage
import akka.dispatch.Await
import akka.actor._

/**
 * @author eiennohito
 * @since 05.04.12
 */

trait LiftMessage extends KotonohaMessage
case class BindLiftActor(actor: CometActor)
case class UnbindLiftActor(actor: CometActor)
case object Ping extends LiftMessage
case object Shutdown extends LiftMessage

trait AkkaInterop extends CometActor with Akka {
  def createBridge(): ActorRef = {
    import akka.util.duration._
    val f = apa(akkaServ.root, BindLiftActor(this))(5 seconds)
    Await.result(f.mapTo[ActorRef], 5 seconds)
  }

  lazy implicit protected val sender: ActorRef = createBridge()

  override protected def localSetup() = {
    sender.tell(Ping) //compute lazy parameter
    super.localSetup()
  }

  override protected def localShutdown() = {
    sender.tell(Shutdown)
    super.localShutdown()
  }
}

case class ToAkka(actor: ActorRef, in: Any)

class LiftBridge(svc: ActorRef, lift: CometActor) extends Actor {
  protected def receive = {
    case ToAkka(ar, msg) => ar ! msg
    case Ping => //do nothing
    case Shutdown =>  {
      svc ! UnbindLiftActor(lift)
    }
    case x if !x.isInstanceOf[AutoReceivedMessage] => lift ! x
  }
}

class LiftActorService extends Actor {
  val actors = new collection.mutable.HashMap[CometActor, ActorRef]

  protected def receive = {
    case BindLiftActor(lift) => {
      val ar = context.actorOf(Props(new LiftBridge(self, lift)))
      actors += (lift -> ar)
      sender ! ar
    }
    case UnbindLiftActor(lift) => {
      actors.remove(lift) map { _ ! PoisonPill }
    }
  }
}
