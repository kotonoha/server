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

import net.liftweb.common.Box
import ws.kotonoha.server.records.{LifetimeObj, QrEntry, Lifetime}
import org.bson.types.ObjectId
import concurrent.duration.FiniteDuration

case class RegisterLifetime[T <: Lifetime](obj: T, duration: FiniteDuration) extends LifetimeMessage
case object FindStaleLifetimeObjs extends LifetimeMessage

class LifetimeActor extends KotonohaActor {
  import concurrent.duration._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def registerLifetime(value: Lifetime, duration: FiniteDuration) = {
    val rec = LifetimeObj.createRecord
    rec.obj(value.recid).objtype(value.lifetimeObj).deadline(now.plus(duration))
    services ! SaveRecord(rec)
  }

  def findStale = {
    val stale = LifetimeObj where (_.deadline lt now) fetch()
    val objs = stale flatMap { case o =>
      val i = o.objtype.get
      val finder = LifetimeObjects.finderFor(i)
      finder(o.obj.get)
    }
    objs.map(_.record).map {case o =>  o.delete_! }
    stale.foreach(services ! DeleteRecord(_))
  }


  override def preStart() {
    context.system.scheduler.schedule(1 minute, 5 minutes, self, FindStaleLifetimeObjs)
  }

  override def receive = {
    case RegisterLifetime(obj, lifetime) => registerLifetime(obj, lifetime)
    case FindStaleLifetimeObjs => findStale
  }

}

object LifetimeObjects extends Enumeration {
  type LiftetimeObjects = LifetimeObjects.Value
  val QrEnt = Value

  def finderFor(v: LifetimeObjects.LiftetimeObjects): (ObjectId => Box[Lifetime]) = v match {
    case QrEnt => QrEntry.find
  }
}
