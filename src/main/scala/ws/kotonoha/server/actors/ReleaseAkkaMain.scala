package ws.kotonoha.server.actors

import auth.ClientRegistry
import learning.WordSelector
import ws.kotonoha.server.learning.EventProcessor
import akka.util.duration._
import akka.actor._
import akka.pattern.ask
import akka.dispatch.{Await, ExecutionContext}
import akka.util.{Timeout, Duration}
import org.bson.types.ObjectId


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

/**
 * @author eiennohito
 * @since 01.02.12
 */

trait AkkaMain {
  def ! (x : AnyRef) = global ! x
  def ? (x: AnyRef) = ask(global, x)(15 seconds)

  def system: ActorSystem

  def global: ActorRef

  def context = ExecutionContext.defaultExecutionContext(system)

  def schedule(f: () => Unit, delay: Duration) = {
    val runnable = new Runnable {
      def run() {
        f()
      }
    }
    system.scheduler.scheduleOnce(delay, runnable)
  }

  def shutdown() {
    system.shutdown()
  }

  def userActorF(uid: ObjectId) = {
    (this ? UserActor(uid)).mapTo[ActorRef]
  }

  def userActor(uid: ObjectId) = {
    Await.result(userActorF(uid), 15 seconds)
  }
}

object ReleaseAkkaMain extends AkkaMain {
  val system = ActorSystem("k")

  lazy val global = system.actorOf(Props[GlobalActor], GlobalActor.globalName)

  system.eventStream.subscribe(
    system.actorOf(Props(new Actor with ActorLogging {
      override def receive = {
        case d: DeadLetter => log.info("Got a " + d)
      }
    })), classOf[DeadLetter]
  )
}
