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

package ws.kotonoha.server.actors.lift

import akka.pattern.{ask => apa}
import net.liftweb.http.CometActor
import ws.kotonoha.server.actors.ioc.Akka
import ws.kotonoha.server.actors.{CreateActor, KotonohaMessage}
import scala.concurrent.Await
import akka.actor._

/**
 * @author eiennohito
 * @since 05.04.12
 */

trait LiftMessage extends KotonohaMessage

case class BindLiftActor(actor: CometActor) extends LiftMessage

case class UnbindLiftActor(actor: CometActor) extends LiftMessage

case object Ping extends LiftMessage

case object Shutdown extends LiftMessage

trait AkkaInterop extends CometActor with Akka {

  import concurrent.duration._

  private def createBridge(): ActorRef = {
    val f = apa(akkaServ.global, BindLiftActor(this))(5 seconds)
    Await.result(f.mapTo[ActorRef], 5 seconds)
  }

  lazy implicit protected val sender: ActorRef = createBridge()

  override protected def localSetup() = {
    sender ! Ping //compute lazy parameter
    super.localSetup()
  }

  override protected def localShutdown() = {
    sender ! Shutdown
    super.localShutdown()
  }

  def createActor(p: Props, name: String = "", parent: ActorRef = sender) = {
    val f = apa(parent, CreateActor(p, name))(30 seconds).mapTo[ActorRef]
    Await.result(f, 30 seconds)
  }

  def toAkka(msg: AnyRef) = akkaServ.global ! msg
}

case class ToAkka(actor: ActorRef, in: Any)

class LiftBridge(svc: ActorRef, lift: CometActor) extends Actor {
  override def receive = {
    case ToAkka(ar, msg) => ar ! msg
    case Ping => //do nothing
    case CreateActor(p, name) => {
      val act = name match {
        case "" | null => context.actorOf(p)
        case nm => context.actorOf(p, nm)
      }
      sender ! act
    }
    case Shutdown => {
      svc ! UnbindLiftActor(lift)
    }
    case x => lift ! x
  }
}

class LiftActorService extends Actor {
  val actors = new collection.mutable.HashMap[CometActor, ActorRef]

  override def receive = {
    case BindLiftActor(lift) =>
      val ar = context.actorOf(Props(new LiftBridge(self, lift)))
      actors += (lift -> ar)
      sender ! ar
    case UnbindLiftActor(lift) =>
      actors.remove(lift) foreach { context.stop }
  }
}
