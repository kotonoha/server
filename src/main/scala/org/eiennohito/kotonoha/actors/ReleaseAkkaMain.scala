package org.eiennohito.kotonoha.actors

import auth.ClientRegistry
import learning.WordSelector
import org.eiennohito.kotonoha.learning.EventProcessor
import akka.util.duration._
import akka.actor._
import akka.pattern.ask
import akka.dispatch.{Await, ExecutionContext}
import akka.util.{Timeout, Duration}


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
  def ! (x : AnyRef) = root ! x
  def ? (x: AnyRef) = ask(root, x)(5 seconds)

  def system: ActorSystem

  def root: ActorRef

  def wordSelector: ActorRef
  def eventProcessor: ActorRef

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

trait RootActor { this: Actor =>
  lazy val root = context.actorFor("/user/root")
}

trait MongoActor { this: Actor =>
  lazy val mongo = context.actorFor("/user/root/mongo")
}


object ReleaseAkkaMain extends AkkaMain {
  val system = ActorSystem("kot")
  
  lazy val root = system.actorOf(Props[RestartActor], "root")
  private val f = ask(root, TopLevelActors)(10 seconds).mapTo[(ActorRef, ActorRef)]
  private val x = Await.result(f, 10 seconds)

  val wordSelector = x._1
  val eventProcessor = x._2
}
