package org.eiennohito.kotonoha.actors

import akka.actor.{Props, ActorSystem}
import learning.WordSelector
import org.eiennohito.kotonoha.learning.{MarkEventProcessor}
import akka.dispatch.ExecutionContext


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

object Akka {
  val system = ActorSystem("kotonoha_system")

  val wordRegistry = system.actorOf(Props[WordRegistry])
  val wordSelector = system.actorOf(Props[WordSelector])
  val markProcessor = system.actorOf(Props[MarkEventProcessor])

  def context = ExecutionContext.defaultExecutionContext(system)

  def shutdown() {
    system.shutdown()
  }
}
