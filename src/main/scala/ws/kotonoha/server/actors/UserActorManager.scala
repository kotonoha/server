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

package ws.kotonoha.server.actors

import org.bson.types.ObjectId
import akka.actor._
import org.joda.time.DateTime
import concurrent.duration._
import scala.Some
import akka.actor.SupervisorStrategy.Restart
import scala.concurrent.{Future, ExecutionContext}

/**
 * @author eiennohito
 * @since 07.01.13 
 */
class UserActorManager extends Actor with ActorLogging {
  import ws.kotonoha.server.util.DateTimeUtils._
  var userMap = Map[ObjectId, ActorRef]()
  var lifetime = Map[ObjectId, DateTime]()

  private def create(uid: ObjectId) = {
    val name = uid.toString
    log.debug(s"trying to create an actor for uid ${name}")
    context.actorOf(Props(new PerUserActor(uid)), name)
  }

  def userActor(uid: ObjectId) = {
    lifetime += uid -> now
    userMap.get(uid) match {
      case Some(a) => a
      case None => {
        val a = create(uid)
        userMap += uid -> a
        a
      }
    }
  }

  def checkLife() {
    val time = now.minusMinutes(15)
    val uids = lifetime.filter(_._2.isBefore(time)).keySet
    val (stale, good) = userMap.partition(c => uids.contains(c._1))
    lifetime --= uids
    userMap = good
    stale.foreach { case (_, a) => a ! PoisonPill }
  }

  implicit val ec: ExecutionContext = context.system.dispatcher


  override def preStart() {
    context.system.scheduler.schedule(15 minutes, 5 minutes, self, Ping)
  }

  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  override def receive = {
    case Ping => checkLife()
    case PingUser(uid) => userActor(uid)
    case UserActor(uid) => sender ! userActor(uid)
    case ForUser(uid, msg) => userActor(uid).forward(msg)
    case AskAllUsers(msg) =>
      val answer = userMap.map {
        case (u, a) => a.ask(msg)(Timeout(5 seconds)).map(u -> _)
      }.toList
      sender ! Future.sequence(answer)
    case TellAllUsers(msg) => userMap.values.foreach(_ ! msg)
    case InitUsers =>
      val actor = userActor(ObjectId.get()) //lasy loading
      context.system.scheduler.scheduleOnce(10 seconds, actor, PoisonPill) //and kill in 10 secs
  }

  case object Ping

  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 hour) {
    case e => log.error(e, "Error in Root user actor"); Restart
  }
}

trait MessageForUser
case class UserActor(uid: ObjectId) extends MessageForUser
case class ForUser(user: ObjectId, msg: AnyRef) extends MessageForUser
case class AskAllUsers(msg: AnyRef) extends MessageForUser
case class TellAllUsers(msg: AnyRef) extends MessageForUser
case class PingUser(uid: ObjectId) extends MessageForUser
case object InitUsers extends MessageForUser
