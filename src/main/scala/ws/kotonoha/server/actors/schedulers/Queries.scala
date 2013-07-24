/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.actors.schedulers

import org.bson.types.ObjectId
import ws.kotonoha.server.records.WordCardRecord

/**
 * @author eiennohito
 * @since 27.02.13 
 */

object Queries {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def badCards(uid: ObjectId) = {
    WordCardRecord.enabledFor(uid) and (_.notBefore lt now) and
      (_.learning.subfield(_.inertia) lt 1.0)
  }

  def newCards(uid: ObjectId) = {
    WordCardRecord.enabledFor(uid) and (_.notBefore lt now) and (_.learning exists false)
  }

  def scheduled(uid: ObjectId) = {
    WordCardRecord.enabledFor(uid) and (_.notBefore lt now) and
      (_.learning.subfield(_.intervalEnd) lt now)
  }
}
