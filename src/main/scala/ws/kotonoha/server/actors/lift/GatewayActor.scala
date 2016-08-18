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
import net.liftweb.http.{BaseCometActor, CometActor}
import ws.kotonoha.server.actors.ioc.Akka
import ws.kotonoha.server.actors.{CreateActor, KotonohaMessage}

import scala.concurrent.Await
import akka.actor._
import com.google.inject.Inject
import ws.kotonoha.server.ioc.IocActors

import scala.reflect.ClassTag

/**
 * @author eiennohito
 * @since 05.04.12
 */

trait LiftMessage extends KotonohaMessage

case class BindLiftActor(actor: BaseCometActor) extends LiftMessage

case class UnbindLiftActor(actor: BaseCometActor) extends LiftMessage

case object Ping extends LiftMessage

case object Shutdown extends LiftMessage


trait AkkaInteropBase extends BaseCometActor with Akka {

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

  def createActor[T <: Actor : ClassTag](name: String = "", parent: ActorRef = sender) = {
    val f = apa(parent, CreateActor(implicitly[ClassTag[T]].runtimeClass, name))(30 seconds).mapTo[ActorRef]
    Await.result(f, 30 seconds)
  }

  def toAkka(msg: AnyRef) = akkaServ.global ! msg
}

trait AkkaInterop extends AkkaInteropBase with CometActor

case class ToAkka(actor: ActorRef, in: Any)

class LiftBridge(svc: ActorRef, lift: BaseCometActor, ioc: IocActors) extends Actor {
  override def receive = {
    case ToAkka(ar, msg) => ar ! msg
    case Ping => //do nothing
    case CreateActor(p, name) => {
      val props = ioc.props(Manifest.classType(p))
      val act = name match {
        case "" | null => context.actorOf(props)
        case nm => context.actorOf(props, nm)
      }
      sender ! act
    }
    case Shutdown =>
      svc ! UnbindLiftActor(lift)
    case x => lift ! x
  }
}

class LiftActorService @Inject() (
  ioc: IocActors
) extends Actor {
  val actors = new collection.mutable.HashMap[BaseCometActor, ActorRef]

  override def receive = {
    case BindLiftActor(lift) =>
      val ar = context.actorOf(Props(new LiftBridge(self, lift, ioc)))
      actors += (lift -> ar)
      sender ! ar
    case UnbindLiftActor(lift) =>
      actors.remove(lift) foreach { context.stop }
  }
}
