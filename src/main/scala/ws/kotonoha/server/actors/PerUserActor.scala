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
import akka.util.duration._
import akka.dispatch.{ExecutionContext, Await}
import akka.util.Timeout

/**
 * @author eiennohito
 * @since 06.01.13 
 */

class PerUserActor(oid: ObjectId) extends Actor {
  val guard = context.actorOf(Props[UserGuardActor], "guard")

  protected def receive = {
    case UserId => sender ! oid
    case x => guard.forward(x)
  }
}

case object UserId

import GlobalActor._

trait KotonohaActor extends Actor {

  def globalPath = context.system.child(globalName)

  def userPath = globalPath / userName

  def svcPath = globalPath / svcName

  lazy val users = {
    context.actorFor(userPath)
  }

  lazy val services = {
    context.actorFor(svcPath)
  }

  implicit val ec: ExecutionContext = context.system.dispatcher
}

trait RootActor { this: KotonohaActor =>
  lazy val root = {
    context.actorFor(globalPath)
  }
}

trait UserScopedActor extends KotonohaActor {
  def userActorPath = {
    val mypath = self.path
    val rootpath = userPath
    val un = mypath.elements.drop(rootpath.elements.size).head
    rootpath / un
  }

  def guardActorPath = {
    userActorPath / "guard"
  }

  def scoped(name: String) = context.actorFor(guardActorPath / name)

  lazy val userActor = {
    context.actorFor(userActorPath)
  }

  lazy val uid = {
    implicit val timeout: Timeout = 1 minute
    val f = (userActor ? UserId).mapTo[ObjectId]
    Await.result(f, 1 minute)
  }
}

class RichAnyRef(o: AnyRef) {
  def u(uid: ObjectId) = ForUser(uid, o)
  def forUser(uid: ObjectId) = u(uid)
}

object UserSupport {
  implicit def anyRef2RichSupport(o: AnyRef) = new RichAnyRef(o)
}
