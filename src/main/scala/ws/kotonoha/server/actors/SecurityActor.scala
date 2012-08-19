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

import akka.actor.{ActorRef, Actor}
import ioc.{ReleaseAkka, Akka}
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import akka.dispatch.{Future, Await}
import ws.kotonoha.server.security.{Roles, GrantRecord}

/**
 * @author eiennohito
 * @since 18.08.12
 */

trait SecurityMessage extends KotonohaMessage
case object GetSecurityActor extends SecurityMessage
case class CheckGrant(user: Long, role: String) extends SecurityMessage
case class GrantStatus(user: Long, role: String, status: Boolean)
case class GrantRole(user: Long, role: String) extends SecurityMessage
case class RevokeRole(user: Long, role: String) extends SecurityMessage

class SecurityActor extends Actor {
  protected def receive = {
    case GetSecurityActor => sender ! self
    case CheckGrant(u, r) => {
      val grant = GrantRecord.haveGrant(u, r)
      sender ! GrantStatus(u, r, grant)
    }
    case GrantRole(u, r) => {
      val rec = GrantRecord.createRecord
      rec.role(r).user(u).save
    }
    case RevokeRole(u, r) => {
      GrantRecord.revokeRole(u, r)
    }
  }
}

object GrantManager extends Akka with ReleaseAkka {
  lazy val actor: ActorRef = {
    implicit val timeout: Timeout = 10 seconds
    val f = (akkaServ ? GetSecurityActor).mapTo[ActorRef]
    Await.result(f, 10 seconds)
  }

  implicit val timeout: Timeout = 5 seconds

  def checkRole(user: Long, role: Roles.Role): Boolean = checkRole(user, role.toString)

  def checkRole(user: Long, role: String): Boolean = {
    val f = checkRoleAsync(user, role)
    Await.result(f, 5 seconds)
  }

  def checkRoleAsync(user: Long, role: String): Future[Boolean] = {
    (actor ? CheckGrant(user, role)).mapTo[GrantStatus].map {
      _.status
    }
  }

  def grantRole(user: Long, role: Roles.Role) = {
    actor ! GrantRole(user, role.toString)
  }

  def revokeRole(user: Long, role: Roles.Role) = {
    actor ! RevokeRole(user, role.toString)
  }
}
