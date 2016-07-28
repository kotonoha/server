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

package ws.kotonoha.server.dict

import java.util.{ArrayList => JList}

import org.bson.types.ObjectId
import ws.kotonoha.akane.pipe.juman.JumanEntry
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.dict.jmdict.LuceneJmdict
import ws.kotonoha.server.actors.recommend.RecommendRequest
import ws.kotonoha.server.dict.kanjidic.Kanjidic
import ws.kotonoha.server.records.dictionary.KanjidicRecord

/**
 * @author eiennohito
 * @since 14.03.13 
 */

case class DoRecommend(writing: String, reading: String, kanji: List[String], kinfo: Map[String, KanjidicRecord], juman: List[JumanEntry])

object DoRecommend {
  def apply(writ: String, read: String, juman: List[JumanEntry]) = {
    val kanji = UnicodeUtil.kanji(writ)
    val nfo = Kanjidic.entries(kanji)
    new DoRecommend(writ, read, kanji, nfo, juman)
  }
}

object WordClassResolver {

  def isSimpleKun(r: DoRecommend): Boolean = {
    if (r.kanji.length != 1) false
    else {
      val k = r.kanji.head
      r.kinfo.get(k) match {
        case None => false
        case Some(ki) =>
          ki.rmgroups.is.flatMap(_.cleanKunyomi).contains(r.reading)
      }
    }
  }
}

object Recommender {
  def kanji(prio: Int) = new KanjiRecommender(prio)
  def kun(prio: Int) = new SingleKunRecommender(prio)
  def on(prio: Int) = new SingleOnRecommender(prio)
  def juku(prio: Int) = new SimpleJukugoRecommender(prio)
  def juman(prio: Int) = new JumanRecommender(prio)
}

class Recommender(uid: ObjectId, jmd: LuceneJmdict, ignores: Seq[Long]) {
  import Recommender._

  val longjuku = List(kanji(100), kun(50), on(30))
  val shortjuku = List(kun(100), on(75), juku(25))
  val single = List(juku(100), juman(70), kun(50), on(50))
  val rest = List(kun(70), on(70), juku(70), juman(50))

  //val ignores = RecommendationIgnore where (_.user eqs uid) select(_.jmdict) fetch()


  def preprocess(cand: RecommendRequest): List[RecommendedSubresult] = {
    (cand.writ, cand.read) match {
      case (None, None) => Nil
      case (Some(writ), None) => //preprocessNoreading(writ, cand.juman)
        Nil
      case (None, Some(read)) => //preprocessNowriting(read, cand.juman)
        Nil
      case (Some(writ), Some(read)) =>
        val ir = DoRecommend(writ, read, cand.juman)
        val selector = if (writ.length == ir.kanji.length) {
          if (writ.length > 2) longjuku else shortjuku
        } else {
          if (ir.kanji.length == 1) single else rest
        }
        val selectors = selector.flatMap(x => x.apply(ir))
        selectors.flatMap(x => x.select(jmd, ignores))
    }
  }

  def process(cand: RecommendRequest) = {
    val data = preprocess(cand)
    data.sortBy(_.prio).distinct
  }
}
