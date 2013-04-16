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
import concurrent.ExecutionContext

/**
 * @author eiennohito
 * @since 07.01.13 
 */
class UserActorManager extends Actor with ActorLogging {
  import ws.kotonoha.server.util.DateTimeUtils._
  var userMap = Map[ObjectId, ActorRef]()
  var lifetime = Map[ObjectId, DateTime]()

  def create(uid: ObjectId) = {
    val name = uid.toString
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
    val time = now minusMinutes (15)
    val uids = lifetime.filter(_._2.isBefore(time)).map(_._1).toSet
    val (stale, good) = userMap.partition(c => uids.contains(c._1))
    lifetime --= uids
    userMap = good
    stale.foreach { case (_, a) => a ! PoisonPill }
  }

  implicit val ec: ExecutionContext = context.system.dispatcher


  override def preStart() {
    context.system.scheduler.schedule(15 minutes, 5 minutes, self, Ping)
  }

  override def receive = {
    case Ping => checkLife()
    case PingUser(uid) => lifetime.get(uid) foreach( lifetime += uid -> _ )
    case UserActor(uid) => sender ! userActor(uid)
    case ForUser(uid, msg) => userActor(uid).forward(msg)
    case InitUsers =>
      val actor = create(new ObjectId()) //lasy loading
      context.system.scheduler.scheduleOnce(10 seconds, actor, PoisonPill) //and kill in 10 secs
  }

  case object Ping

  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 hour) {
    case e => log.error(e, "Error in Root user actor"); Restart
  }
}

case class UserActor(uid: ObjectId)
case class ForUser(user: ObjectId, msg: AnyRef)
case class PingUser(uid: ObjectId)
case object InitUsers
