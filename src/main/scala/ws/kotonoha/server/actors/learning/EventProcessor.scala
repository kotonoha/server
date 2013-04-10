package ws.kotonoha.server.learning

import net.liftweb.common.Full
import akka.util.Timeout
import akka.actor.{Terminated, ActorRef, ActorLogging, Props}
import ws.kotonoha.server.records.{WordCardRecord, ItemLearningDataRecord}
import ws.kotonoha.server.actors._
import model.{ScheduleLater, SchedulePaired, ChangeWordStatus, ChangeCardEnabled}
import ws.kotonoha.server.supermemo.{SM6, ProcessMark}
import concurrent.Future
import com.mongodb.casbah.WriteConcern
import ws.kotonoha.server.records.events.{ChangeWordStatusEventRecord, MarkEventRecord}
import org.bson.types.ObjectId
import ws.kotonoha.server.records.misc.CardSchedule

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
import concurrent.duration._

trait EventMessage extends KotonohaMessage

case class ProcessMarkEvents(marks: List[MarkEventRecord]) extends EventMessage

case class ProcessMarkEvent(mark: MarkEventRecord) extends EventMessage

case class RegisterServices(mong: ActorRef, sched: ActorRef) extends EventMessage

case class ProcessWordStatusEvent(ev: List[ChangeWordStatusEventRecord]) extends EventMessage

case class ProcessWordStatus(ev: ChangeWordStatusEventRecord) extends EventMessage

case class RegisterCardListener(actor: ActorRef) extends EventMessage

case class UnregisterCardListener(actor: ActorRef) extends EventMessage

case class CardProcessed(cid: ObjectId)


class ChildProcessor extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  implicit val timeout = Timeout(5000 milliseconds)

  var mongo: ActorRef = context.actorFor(guardActorPath / "mongo")
  var sm6: ActorRef = context.actorOf(Props[SM6], "sm6")

  def processWs(ws: ChangeWordStatusEventRecord) = {
    mongo ! SaveRecord(ws)
    userActor ! ChangeCardEnabled(ws.word.is, false)
    userActor ! ChangeWordStatus(ws.word.is, ws.toStatus.is)
    sender ! 1
  }

  override def receive = {
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

    val scheds = CardSchedule where (_.user eqs card.user.is) and (_.card eqs card.id.is) orderDesc(_.date) fetch()
    if (scheds.length > 1) {
      log.warning("multiple schedules: {}", scheds)
    }

    if (!scheds.isEmpty) {
      val head = scheds.head
      mr.source(head.source.is)
      mr.seq(head.seq.is)
      mr.bundle(head.bundle.is)

      scheds.foreach { x => mongo ! DeleteRecord(x) }
    }

    mongo ! SaveRecord(mr)
  }

  def processMark(ev: MarkEventRecord): Unit = {
    val obj = WordCardRecord where (_.user eqs (ev.user.is)) and (_.id eqs ev.card.is) get()
    obj match {
      case None => {
        log.warning("invalid mark event: {}", ev)
        sender ! 0
      }
      case Some(card) => {
        saveMarkRecord(ev, card)
        userActor ! SchedulePaired(card.word.is, card.cardMode.is)
        if (ev.mark.is < 3.5) {
          userActor ! ScheduleLater(card.id.is, 3 hours)
        }
        val it = ProcessMark(card.learning.is, ev.mark.is, ev.datetime.is, card.user.is, card.id.is)
        val cardF = (sm6 ? it).mapTo[ItemLearningDataRecord]
        val ur = cardF.map {
          l => {
            log.debug(s"updated learning to $l")
            WordCardRecord where (_.id eqs (ev.card.is)) modify (_.learning setTo (l)) updateOne (WriteConcern.Safe)
          }
        }
        ur map {
          x =>
            log.debug("processed event for cardid={}", card.id.is)
            1
        } pipeTo (sender)
      }
    }
  }
}


class EventProcessor extends UserScopedActor with ActorLogging {
  implicit val dispatcher = context.dispatcher
  lazy val children = context.actorOf(Props[ChildProcessor], "child")
  implicit val timeout: Timeout = 5 seconds

  var listeners: List[ActorRef] = Nil

  override def receive = {
    case RegisterCardListener(act) =>
      listeners ::= act
      context.watch(act)
    case Terminated(act) => listeners = listeners.filter(_ != act)
    case UnregisterCardListener(act) => listeners = listeners.filter(_ != act)
    case p: ProcessMarkEvent =>
      val f = (children ? p)
      f foreach {
        _ =>
          listeners.foreach(a => a ! CardProcessed(p.mark.card.is))
      }
      f pipeTo sender
    case ProcessMarkEvents(evs) => {
      val futs = evs.map {
        ev => ask(self, ProcessMarkEvent(ev)).mapTo[Int]
      }
      Future.sequence(futs) pipeTo sender
    }
    case ProcessWordStatusEvent(evs) => {
      val f = evs.map(e => ask(children, ProcessWordStatus(e)).mapTo[Int])
      Future.sequence(f) pipeTo sender
    }
  }
}
