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

package org.eiennohito.kotonoha.actors

import akka.actor.{Actor, ActorRef}
import net.liftweb.common.Box
import akka.util.FiniteDuration
import org.eiennohito.kotonoha.records.{LifetimeObj, QrEntry, Lifetime}

case class RegisterLifetime[T <: Lifetime](obj: T, duration: FiniteDuration) extends LifetimeMessage
case object FindStaleLifetimeObjs extends LifetimeMessage

class LifetimeActor extends Actor with RootActor {
  import akka.util.duration._
  import com.foursquare.rogue.Rogue._
  import org.eiennohito.kotonoha.util.DateTimeUtils._

  def registerLifetime(value: Lifetime, duration: FiniteDuration) = {
    val rec = LifetimeObj.createRecord
    rec.obj(value.recid).objtype(value.lifetimeObj).deadline(now.plus(duration))
    root ! SaveRecord(rec)
  }

  def findStale = {
    val stale = LifetimeObj where (_.deadline before now) fetch()
    val objs = stale flatMap { case o =>
      val i = o.objtype.is
      val finder = LifetimeObjects.finderFor(i)
      finder(o.obj.is)
    }
    objs.map(_.record).map {case o =>  o.delete_! }
    stale.foreach(root ! DeleteRecord(_))
  }


  override def preStart() {
    context.system.scheduler.schedule(10 seconds, 10 seconds, self, FindStaleLifetimeObjs)
  }

  protected def receive = {
    case RegisterLifetime(obj, lifetime) => registerLifetime(obj, lifetime)
    case FindStaleLifetimeObjs => findStale
  }

}

object LifetimeObjects extends Enumeration {
  type LiftetimeObjects = LifetimeObjects.Value
  val QrEnt = Value

  def finderFor(v: LifetimeObjects.LiftetimeObjects): (Long => Box[Lifetime]) = v match {
    case QrEnt => QrEntry.find
  }
}
