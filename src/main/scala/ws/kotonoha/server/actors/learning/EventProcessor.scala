/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.actors.learning

import akka.actor.{ActorLogging, ActorRef}
import akka.util.Timeout
import com.google.inject.Inject
import org.bson.types.ObjectId
import ws.kotonoha.server.actors._
import ws.kotonoha.server.actors.model.{ChangeCardEnabled, ChangeWordStatus}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.events.{ChangeWordStatusEventRecord, MarkEventRecord}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * @author eiennohito
 * @since 01.02.12
 */

import akka.pattern._

import scala.concurrent.duration._

trait EventMessage extends KotonohaMessage

case class ProcessMarkEvents(marks: List[MarkEventRecord]) extends EventMessage

case class ProcessMarkEvent(mark: MarkEventRecord) extends EventMessage

case class RegisterServices(mong: ActorRef, sched: ActorRef) extends EventMessage

case class ProcessWordStatusEvent(ev: List[ChangeWordStatusEventRecord]) extends EventMessage

case class ProcessWordStatus(ev: ChangeWordStatusEventRecord) extends EventMessage

case class CardProcessed(cid: ObjectId)


class ChildProcessor @Inject() (
  meo: MarkEventOps
) extends UserScopedActor with ActorLogging {
  implicit val timeout = Timeout(5000 milliseconds)

  import ActorUtil.aOf

  var mongo: ActorRef = context.actFor(guardActorPath / "mongo")

  def processWs(ws: ChangeWordStatusEventRecord) = {
    mongo ! SaveRecord(ws)
    userActor ! ChangeCardEnabled(ws.word.get, false)
    userActor ! ChangeWordStatus(ws.word.get, ws.toStatus.get)
    sender ! 1
  }

  override def receive = {
    case ProcessMarkEvent(ev) => processMark(ev)
    case ProcessWordStatus(ev) => processWs(ev)
  }

  def processMark(ev: MarkEventRecord): Unit = {
    val sndr = sender()
    meo.process(ev).onComplete {
      case Success(_) => sndr ! 1
      case Failure(t) =>
        log.error(t, "error processing event {}", ev)
        sndr ! 0
    }
  }
}


class EventProcessor @Inject() (
  uc: UserContext
) extends UserScopedActor with ActorLogging {
  implicit val dispatcher = context.dispatcher
  lazy val children = context.actorOf(uc.props[ChildProcessor], "child")
  implicit val timeout: Timeout = 5 seconds

  override def receive = {
    case p: ProcessMarkEvent =>
      val f = children ? p
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
