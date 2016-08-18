/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.ops

import com.google.inject.Inject
import ws.kotonoha.server.actors.model.{Candidate, PresentStatus, SimilarWord}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.records.events.AddWordRecord

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/18
  */
class SimilarWordOps @Inject() (
  rm: RMData,
  uc: UserContext
)(implicit ec: ExecutionContext) {
  def uid = uc.uid

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def countWordsFor(c: Candidate) = {
    val q =
      WordRecord.where(_.user eqs uid).and(_.writing eqs c.writing)
          .andOpt(c.reading)((w, r) => w.reading eqs r)
    rm.count(q)
  }

  def similarWords(c: Candidate, limit: Int = 10): Future[List[SimilarWord]] = {
    val init = WordRecord where (_.user eqs uid)
    val q = if (c.reading.isDefined) { //reading
      init and (_.reading eqs c.reading.get)
    } else init and (_.writing eqs c.writing) //or writing?

    val sq = q.select(_.id, _.writing, _.reading).limit(limit)
    rm.fetch(sq).map(x => x.map(SimilarWord.tupled))
  }

  def similarAdds(c: Candidate, limit: Int = 10): Future[List[SimilarWord]] = {
    val init = AddWordRecord where (_.user eqs uid) and (_.processed eqs false)
    val q = if (c.reading.isDefined) {
      init and (_.reading eqs c.reading.get)
    } else init and (_.writing eqs c.writing)
    val lq = q.select(_.id, _.writing, _.reading).limit(limit)

    rm.fetch(lq).map { l => l.map {
      case (id, w, ro) => SimilarWord(id, if (w == "") Nil else w :: Nil, ro.toList)
    }}
  }

  def similarRegistered(c: Candidate, cnt: Int = 10): Future[PresentStatus] = {
    val words = similarWords(c, cnt)
    val adds = similarAdds(c, cnt)
    for {
      w <- words
      a <- adds
    } yield PresentStatus(c, w, a)
  }
}
