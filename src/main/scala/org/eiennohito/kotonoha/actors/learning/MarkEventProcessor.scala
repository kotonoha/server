package org.eiennohito.kotonoha.learning

import akka.actor.{ActorLogging, Props, Actor}
import org.eiennohito.kotonoha.utls.DateTimeUtils._
import org.eiennohito.kotonoha.supermemo.{SM6, ItemUpdate}
import net.liftweb.common.{Failure, Empty, Full}
import org.eiennohito.kotonoha.actors.learning.{SchedulePaired, CardScheduler}
import org.eiennohito.kotonoha.actors.{UpdateRecord, SaveRecord, MongoDBActor}
import org.eiennohito.kotonoha.records.{ItemLearningDataRecord, MarkEventRecord}
import akka.util.Timeout
import akka.dispatch.Future

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

case class ProcessMarkEvents(marks: List[MarkEventRecord])
case class ProcessMarkEvent(mark: MarkEventRecord)

class MarkEventProcessor extends Actor with ActorLogging {
  import akka.pattern._
  import akka.util.duration._
  val sched = context.actorOf(Props[CardScheduler], "scheduler")
  var mongo = context.actorOf(Props[MongoDBActor], "mongoActor")
  
  implicit val timeout = Timeout(50 milli)

  protected def receive = {
    case ProcessMarkEvents(evs) =>  {
      val futs = evs.map { ev => ask(self, ev)(100 milli).mapTo[Int]}
      val res = Future.fold(futs)(0)(_+_)(context.dispatcher)
      val se = sender
      res foreach (se ! _)
    }
    case ProcessMarkEvent(ev) => {
      mongo ! SaveRecord(ev)
      ev.card.obj match {
        case Empty =>  {
          log.debug("invalid mark event: {}", ev)
          sender ! 0
        }
        case Full(card) => {
          val sc = sched ? SchedulePaired(card.word.is, card.cardMode.is)
          val it = ItemUpdate(card.learning.is, ev.mark.is, ev.datetime.is, card.user.is)
          card.learning(SM6.update(it))
          val ur = mongo ? UpdateRecord(card)
          val se = sender
          sc.zip(ur) foreach {x => se ! 1}
        }
        case Failure(msg, e, c) => log.error(e.openTheBox, msg); sender ! 0
      }
    }
  }
}
