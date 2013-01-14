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

package ws.kotonoha.server.test

import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import java.util.concurrent.atomic.AtomicInteger
import org.bson.types.ObjectId
import akka.util.Timeout
import concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import akka.testkit.TestActorRef
import ws.kotonoha.server.actors.{CreateActor, UserScopedActor, GlobalActor, AkkaMain}

/**
 * @author eiennohito
 * @since 08.01.13 
 */

object KotonohaTestAkka {
  val counter = new AtomicInteger(0)
}

class KotonohaTestAkka extends AkkaMain {
  val system = ActorSystem("koto-test" + KotonohaTestAkka.counter.getAndAdd(1))

  lazy val global = system.actorOf(Props[GlobalActor], GlobalActor.globalName)

  def userContext(uid: ObjectId) = new UserContext(this, uid)

  private[test] val cnt = new AtomicInteger()
}

class SupervisorActor extends UserScopedActor {
  override def receive = {
    case Nil => //
  }
}

class UserContext(akka: KotonohaTestAkka, uid: ObjectId) {
  private implicit val timeout: Timeout = 10 minutes
  private implicit lazy val system = akka.system
  lazy val actor = akka.userActor(uid)

  private lazy val supervisor = {
    Await.result((actor ? CreateActor(Props[SupervisorActor], s"supervisor${akka.cnt.getAndAdd(1)}"))
      .mapTo[ActorRef], 1 minute)
  }

  def userActor(props: Props, name: String) = {
    TestActorRef(props, supervisor, name)
  }

  def userActor[T <: UserScopedActor](name: String)(implicit m: Manifest[T]) = {
    TestActorRef.apply[T](Props[T], supervisor, name)
  }

  def svcActor(props: Props, name: String) = {
    TestActorRef(props, akka.global, name)
  }
}
