package ws.kotonoha.server.actors

import concurrent.duration._
import akka.actor._
import akka.pattern.ask
import org.bson.types.ObjectId
import ws.kotonoha.server.ioc.KotonohaIoc

import concurrent.{Await, ExecutionContext}


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
  def !(x: AnyRef) = global ! x

  def ?(x: AnyRef) = ask(global, x)(15 seconds)

  def system: ActorSystem

  def global: ActorRef

  def context: ExecutionContext = system.dispatcher

  def schedule(f: () => Unit, delay: FiniteDuration) = {
    val runnable = new Runnable {
      def run() {
        f()
      }
    }
    system.scheduler.scheduleOnce(delay, runnable)(context)
  }

  def shutdown() {}

  def userActorF(uid: ObjectId) = {
    (this ? UserActor(uid)).mapTo[ActorRef]
  }

  def userActor(uid: ObjectId) = {
    Await.result(userActorF(uid), 15 seconds)
  }
}

object ReleaseAkkaMain extends AkkaMain {
  def init(ioc: KotonohaIoc) = {
    system = ioc.spawn[ActorSystem]
    global = ioc.spawn[GlobalActors].global

    system.eventStream.subscribe(
      system.actorOf(Props(new Actor with ActorLogging {
        override def receive = {
          case d: DeadLetter => log.info("Got a " + d)
        }
      })), classOf[DeadLetter])
  }

  @volatile var system: ActorSystem = null

  @volatile var global: ActorRef = null
}
