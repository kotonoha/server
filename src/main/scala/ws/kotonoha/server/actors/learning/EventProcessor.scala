package ws.kotonoha.server.learning

import ws.kotonoha.server.util.DateTimeUtils._
import net.liftweb.common.{Failure, Empty, Full}
import akka.util.Timeout
import akka.dispatch.Future
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import ws.kotonoha.server.records.{ChangeWordStatusEventRecord, ItemLearningDataRecord, MarkEventRecord}
import ws.kotonoha.server.actors.model.{ChangeWordStatus, ChangeCardEnabled, SchedulePaired}
import ws.kotonoha.server.actors._
import ws.kotonoha.server.supermemo.{SMParentActor, SM6, ItemUpdate}

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

trait EventMessage extends KotonohaMessage
case class ProcessMarkEvents(marks: List[MarkEventRecord]) extends EventMessage
case class ProcessMarkEvent(mark: MarkEventRecord) extends EventMessage
case class RegisterServices(mong: ActorRef, sched: ActorRef) extends EventMessage
case class ProcessWordStatusEvent(ev: List[ChangeWordStatusEventRecord]) extends EventMessage
case class ProcessWordStatus(ev: ChangeWordStatusEventRecord) extends EventMessage


class ChildProcessor extends Actor with ActorLogging with RootActor {
  implicit val timeout = Timeout(500 milliseconds)    
  
  var mongo : ActorRef = _
  var sched : ActorRef = _
  var sm6 : ActorRef = context.actorOf(Props[SMParentActor])

  def processWs(ws: ChangeWordStatusEventRecord) = {
    mongo ! SaveRecord(ws)
    root ! ChangeCardEnabled(ws.word.is, false)
    root ! ChangeWordStatus(ws.word.is, ws.toStatus.is)
    sender ! 1
  }

  protected def receive = {
    case ProcessMarkEvent(ev) => processMark(ev)
    case ProcessWordStatus(ev) => processWs(ev)

    case x: RegisterServices => {
      mongo = x.mong
      sched = x.sched
    }
  }

  def processMark(ev: MarkEventRecord) {
    mongo ! SaveRecord(ev)
    val obj = ev.card.obj
    obj match {
      case Empty => {
        log.debug("invalid mark event: {}", ev)
        sender ! 0
      }
      case Full(card) => {
        val sc = sched ? SchedulePaired(card.word.is, card.cardMode.is)
        val it = ItemUpdate(card.learning.is, ev.mark.is, ev.datetime.is, card.user.is, card.id.is)
        val cardF = (sm6 ? it).mapTo[ItemLearningDataRecord].map(card.learning(_))
        val ur = cardF.flatMap {
          c => mongo ? UpdateRecord(c)
        }
        val se = sender
        sc.zip(ur) foreach {
          log.debug("processed event for cardid={}", card.id.is)
          x => se ! 1
        }
      }
      case Failure(msg, e, c) => log.error(e.openTheBox, msg); sender ! 0
    }
  }
}



class EventProcessor extends Actor with ActorLogging with MongoActor with RootActor {
  implicit val dispatcher = context.dispatcher
  lazy val children = context.actorOf(Props[ChildProcessor])

  override def preStart() {
    children ! RegisterServices(mongo, root)
  }
  protected def receive = {
    case p : ProcessMarkEvent => children.forward(p)
    case ProcessMarkEvents(evs) => {
      val futs = evs.map { ev => ask(children, ProcessMarkEvent(ev))(1 second).mapTo[Int]}
      Future.sequence(futs) pipeTo sender
    }
    case ProcessWordStatusEvent(evs) => {
      val f = evs.map(e => ask(children, ProcessWordStatus(e))(1 second).mapTo[Int])
      Future.sequence(f) pipeTo sender
    }
  }
}