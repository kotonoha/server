package org.eiennohito.kotonoha.learning

import org.eiennohito.kotonoha.util.DateTimeUtils._
import org.eiennohito.kotonoha.supermemo.{SM6, ItemUpdate}
import net.liftweb.common.{Failure, Empty, Full}
import org.eiennohito.kotonoha.actors.learning.{SchedulePaired, CardScheduler}
import org.eiennohito.kotonoha.records.{ItemLearningDataRecord, MarkEventRecord}
import akka.util.Timeout
import akka.dispatch.Future
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import akka.routing.{Broadcast, RoundRobinRouter}
import org.eiennohito.kotonoha.actors.{RegisterMongo, UpdateRecord, SaveRecord, MongoDBActor}

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

import akka.pattern._
import akka.util.duration._

case class ProcessMarkEvents(marks: List[MarkEventRecord])
case class ProcessMarkEvent(mark: MarkEventRecord)
case class RegisterServices(mong: ActorRef, sched: ActorRef)

class ChildProcessor extends Actor with ActorLogging {
  implicit val timeout = Timeout(500 milliseconds)    
  
  var mongo : ActorRef = _
  var sched : ActorRef = _
  var sm6 : ActorRef = context.actorOf(Props[SM6])
  
  protected def receive = {
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
          val cardF = (sm6 ? it).mapTo[ItemLearningDataRecord].map(card.learning(_))
          val ur = cardF.flatMap {c => mongo ? UpdateRecord(c)}
          val se = sender
          sc.zip(ur) foreach {x => se ! 1}
        }
        case Failure(msg, e, c) => log.error(e.openTheBox, msg); sender ! 0
      }
    }
    case x: RegisterServices => {
      mongo = x.mong
      sched = x.sched
      sm6 ! RegisterMongo(mongo)
    }
  }
}

class MarkEventProcessor extends Actor with ActorLogging {
  val sched = context.actorOf(Props[CardScheduler], "scheduler")
  lazy val mongo = context.actorFor("/root/mongo")
  val children = context.actorOf(Props[ChildProcessor].withRouter(RoundRobinRouter(nrOfInstances = 4)))


  override def preStart() {
    children ! Broadcast(RegisterServices(mongo, sched))
  }

  protected def receive = {
    case ProcessMarkEvents(evs) =>  {
      implicit val dispatcher = context.dispatcher
      val futs = evs.map { ev => ask(children, ProcessMarkEvent(ev))(1 second).mapTo[Int]}
      val res = Future.sequence(futs)
      val se = sender
      res foreach (se ! _)
    }
  }
}
