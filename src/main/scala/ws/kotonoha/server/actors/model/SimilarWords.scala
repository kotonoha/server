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

package ws.kotonoha.server.actors.model

import org.bson.types.ObjectId
import ws.kotonoha.server.web.comet.Candidate
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.records.events.AddWordRecord

/**
 * @author eiennohito
 * @since 14.03.13 
 */

case class SimilarWord(id: ObjectId, writings: List[String], readings: List[String])

object SimilarWords {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def noWords(id: ObjectId, c: Candidate) = {
    val q = WordRecord where (_.user eqs id) and (_.writing eqs c.writing)
    val cnt = q.andOpt(c.reading)((a, b) => a.reading eqs (b)) count()
    cnt == 0
  }

  def noAdds(uid: ObjectId, c: Candidate) = {
    val q = AddWordRecord where (_.user eqs uid) and (_.writing eqs c.writing)
    val cnt = q.andOpt(c.reading)((m,r) => m.reading eqs r) count()
    cnt == 0
  }

  def similarWords(uid: ObjectId, c: Candidate) = {
    val init = WordRecord where (_.user eqs uid)
    val q = if (c.reading.isDefined) {
      init and (_.reading eqs c.reading.get)
    } else init and (_.writing eqs c.writing)
    q.select(_.id, _.writing, _.reading).fetch(10).map(SimilarWord.tupled)
  }

  def similarAdds(uid: ObjectId, c: Candidate) = {
    val init = AddWordRecord where (_.user eqs uid) and (_.processed eqs false)
    val q = if (c.reading.isDefined) {
      init and (_.reading eqs c.reading.get)
    } else init and (_.writing eqs c.writing)
    q.select(_.id, _.writing, _.reading) fetch(10) map {
      case (id, w, ro) => SimilarWord(id, if (w == "") Nil else w :: Nil, ro.toList)
    }
  }

}
