package org.eiennohito.kotonoha.actors

import learning.WordSelector
import org.eiennohito.kotonoha.learning.{MarkEventProcessor}
import akka.util.duration._
import akka.util.Duration
import akka.actor._
import akka.pattern.ask
import akka.dispatch.{Await, ExecutionContext}


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
  val system: ActorSystem

  val wordRegistry: ActorRef
  val wordSelector: ActorRef
  val markProcessor: ActorRef

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
}


case object TopLevelActors


class RestartActor extends Actor {
  import SupervisorStrategy._
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 1500, withinTimeRange = 1 day) {
    case _: Exception => Restart
  }
  
  val wordRegistry = context.actorOf(Props[WordRegistry])
  val wordSelector = context.actorOf(Props[WordSelector])
  val markProcessor = context.actorOf(Props[MarkEventProcessor])

  protected def receive = {
    case TopLevelActors => {
      sender ! (wordRegistry, wordSelector, markProcessor)
    }
  }
}


object ReleaseAkkaMain extends AkkaMain {
  val system = ActorSystem("kotonoha_system")
  
  val root = system.actorOf(Props[RestartActor], "root")
  val f = ask(root, TopLevelActors)(1 second).mapTo[(ActorRef, ActorRef, ActorRef)]
  val x = Await.result(f, 1 second)

  val wordRegistry = x._1
  val wordSelector = x._2
  val markProcessor = x._3
}
