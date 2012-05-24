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

package org.eiennohito.kotonoha.supermemo

import akka.actor.{Props, ActorRef, Actor}
import org.joda.time.{Duration, DateTime}

/**
 * @author eiennohito
 * @since 24.05.12
 */

case class TimeoutSM6(user: Long, time: DateTime)
case object TerminateSM6

/**
 * Parent actor for SM6 actors, stores and manages lifteime for child objects.
 */
class SMParentActor extends Actor {
  import akka.util.duration._
  import org.eiennohito.kotonoha.util.DateTimeUtils._
  private var active: Map[Long, ActorRef] = Map()

  def createChildFor(userId: Long): ActorRef = {
    val actor = context.actorOf(Props(new SM6(userId)))
    active += userId -> actor
    actor
  }

  protected def receive = {
    case i: ItemUpdate => {
      active.get(i.userId).getOrElse(createChildFor(i.userId)) forward (i)
      context.system.scheduler.scheduleOnce(5 minutes, self, TimeoutSM6(i.userId, now plus (5 minutes)))
    }
    case TimeoutSM6(user, time) => {
      val dur = new Duration(time, now)
      if (dur.getMillis <= 0) {
        active get (user) map { _ ! TerminateSM6 }
        active -= user
      } else {
        context.system.scheduler.scheduleOnce(1 minute, self, TimeoutSM6(user, now plus (1 minute)))
      }
    }
  }
}
