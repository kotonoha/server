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

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import com.google.inject.Inject
import org.bson.types.ObjectId
import ws.kotonoha.server.ioc.{IocActors, UserContextService}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author eiennohito
 * @since 07.01.13 
 */
class UserActorManager @Inject()(ioc: IocActors) extends Actor with ActorLogging {
  private val ucs = ioc.inst[UserContextService]
  val userMap = new mutable.HashMap[ObjectId, ActorRef]()

  private def create(uid: ObjectId) = {
    val name = uid.toString
    log.debug(s"trying to create an actor for uid $name")
    context.actorOf(ucs.of(uid).props[UserGuardActor], name)
  }

  def userActor(uid: ObjectId) = {
    val id = ucs.of(uid).uid
    userMap.get(id) match {
      case Some(a) => a
      case None =>
        val a = create(uid)
        userMap.put(uid, a)
        a
    }
  }

  implicit val ec: ExecutionContext = context.system.dispatcher


  import akka.pattern.ask
  import akka.util.Timeout

  import scala.concurrent.duration._

  override def receive = {
    case UserActor(uid) => sender ! userActor(uid)
    case ForUser(uid, msg) => userActor(uid).forward(msg)
    case AskAllUsers(msg) =>
      val answer = userMap.map {
        case (u, a) => a.ask(msg)(Timeout(5.seconds)).map(u -> _)
      }.toList
      sender ! Future.sequence(answer)
    case TellAllUsers(msg) => userMap.values.foreach(_ ! msg)
    case InitUsers =>
      val uid = ObjectId.get()
      userActor(uid)
    case InvalidateUserActor(uid) =>
      userMap.remove(uid) match {
        case None => log.warning("there was no user actor for uid={}", uid)
        case Some(ar) =>
          log.debug(s"stopping user actor for $uid")
          context.stop(ar)
      }
  }

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
case class InvalidateUserActor(uid: ObjectId) extends MessageForUser
