package ws.kotonoha.server.learning

import ws.kotonoha.server.util.DateTimeUtils._
import net.liftweb.common.{Failure, Empty, Full}
import akka.util.Timeout
import akka.dispatch.Future
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import ws.kotonoha.server.records.{WordCardRecord, ChangeWordStatusEventRecord, ItemLearningDataRecord, MarkEventRecord}
import ws.kotonoha.server.actors.model.{ChangeWordStatus, ChangeCardEnabled, SchedulePaired}
import ws.kotonoha.server.actors._
import ws.kotonoha.server.supermemo.{SM6, ProcessMark}

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


class ChildProcessor extends UserScopedActor with ActorLogging {
  import com.foursquare.rogue.Rogue._
  implicit val timeout = Timeout(5000 milliseconds)
  
  var mongo : ActorRef = context.actorFor(guardActorPath / "mongo")
  var sm6 : ActorRef = context.actorOf(Props[SM6], "sm6")

  def processWs(ws: ChangeWordStatusEventRecord) = {
    mongo ! SaveRecord(ws)
    userActor ! ChangeCardEnabled(ws.word.is, false)
    userActor ! ChangeWordStatus(ws.word.is, ws.toStatus.is)
    sender ! 1
  }

  protected def receive = {
    case ProcessMarkEvent(ev) => processMark(ev)
    case ProcessWordStatus(ev) => processWs(ev)
  }

  def saveMarkRecord(mr: MarkEventRecord, card: WordCardRecord) = {
    val cl = card.learning.valueBox
    cl match {
      case Full(l) => {
        mr.diff(l.difficulty.is)
        mr.interval(l.intervalLength.is)
        mr.rep(l.repetition.is)
        mr.lapse(l.lapse.is)
      }
      case _ => {
        mr.rep(0)
        mr.lapse(0)
      }
    }
    mongo ! SaveRecord(mr)
  }

  def processMark(ev: MarkEventRecord) {
    val obj = WordCardRecord where (_.user eqs(ev.user.is)) and (_.id eqs ev.card.is) get()
    obj match {
      case None => {
        log.debug("invalid mark event: {}", ev)
        sender ! 0
      }
      case Some(card) => {
        saveMarkRecord(ev, card)
        userActor ! SchedulePaired(card.word.is, card.cardMode.is)
        val it = ProcessMark(card.learning.is, ev.mark.is, ev.datetime.is, card.user.is, card.id.is)
        val cardF = (sm6 ? it).mapTo[ItemLearningDataRecord]
        val ur = cardF.map { l => {
          val c = card.learning(l)
          c.save
        }}
        val se = sender
        ur foreach {
          x => {
            log.debug("processed event for cardid={}", card.id.is)
            se ! 1
          }
        }
      }
    }
  }
}



class EventProcessor extends UserScopedActor with ActorLogging {
  implicit val dispatcher = context.dispatcher
  lazy val children = context.actorOf(Props[ChildProcessor], "child")


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
