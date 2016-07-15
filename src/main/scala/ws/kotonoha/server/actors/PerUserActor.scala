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

package ws.kotonoha.server.actors

import akka.actor._
import akka.pattern.ask
import org.bson.types.ObjectId
import concurrent.duration._
import akka.util.Timeout
import concurrent.{Await, ExecutionContext}

/**
 * @author eiennohito
 * @since 06.01.13 
 */

class PerUserActor(oid: ObjectId) extends Actor {
  val guard = context.actorOf(Props[UserGuardActor], "guard")

  override def receive = {
    case UserId => sender ! oid
    case x => guard.forward(x)
  }
}

case object UserId

import GlobalActor._

object ActorUtil {
  def actFor(arp: ActorRefFactory, path: ActorPath, timeout: Timeout = 5.seconds): ActorRef = {
    val sel = arp.actorSelection(path)
    val fut = sel.resolveOne()(timeout)
    Await.result(fut, timeout.duration)
  }

  implicit class aOf(val arp: ActorRefFactory) extends AnyVal {
    def actFor(path: ActorPath, timeout: Timeout = 5.seconds): ActorRef = ActorUtil.actFor(arp, path, timeout)
  }
}

import ActorUtil.aOf

trait KotonohaActor extends Actor {

  def globalPath = context.system.child(globalName)

  def userPath: ActorPath = globalPath / userName

  def svcPath: ActorPath = globalPath / svcName

  lazy val users = {
    context.actFor(userPath)
  }

  lazy val services = {
    context.actFor(svcPath)
  }

  implicit val ec: ExecutionContext = context.system.dispatcher
}

trait RootActor { this: KotonohaActor =>
  lazy val root = {
    context.actFor(globalPath)
  }
}

trait UserScopedActor extends KotonohaActor {
  def userActorPath: ActorPath = {
    val mypath = self.path
    val rootpath = userPath
    val un = mypath.elements.drop(rootpath.elements.size).head
    rootpath / un
  }

  def guardActorPath: ActorPath = {
    userActorPath / "guard"
  }

  def scoped(name: String): ActorRef = context.actFor(guardActorPath / name)

  lazy val userActor: ActorRef = {
    context.actFor(userActorPath)
  }

  lazy val uid = {
    import akka.pattern.ask
    implicit val timeout: Timeout = 1 minute
    val f = (userActor ? UserId).mapTo[ObjectId]
    Await.result(f, 1 minute)
  }
}



object UserSupport {
  import language.implicitConversions
  implicit class RichAnyRef(val o: AnyRef) extends AnyVal {
    def u(uid: ObjectId) = ForUser(uid, o)
    def forUser(uid: ObjectId) = u(uid)
  }
}
