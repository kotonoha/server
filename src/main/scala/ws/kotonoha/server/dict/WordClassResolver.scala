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

package ws.kotonoha.server.dict

import ws.kotonoha.server.records.dictionary.{JMDictRecord, KanjidicRecord}
import ws.kotonoha.server.dict.kanjidic.Kanjidic
import ws.kotonoha.akane.unicode.{KanaUtil, UnicodeUtil}
import net.java.sen.SenFactory
import ws.kotonoha.server.web.comet.Candidate
import java.util.{ArrayList => JList}
import net.java.sen.dictionary.Token
import ws.kotonoha.akane.JumanEntry
import ws.kotonoha.server.records.RecommendationIgnore
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 14.03.13 
 */

/**
 *
 * @param writ Word writing
 * @param read word reading
 * @param juman juman analyze results
 */
case class RecommendRequest(writ: Option[String], read: Option[String], juman: List[JumanEntry])

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
  val validPos = "(^名詞-.*$)|(^動詞-自立$)|(^形容詞-自立$)|(^副詞-.*$)".r

  def kanji(prio: Int) = new KanjiRecommender(prio)
  def kun(prio: Int) = new SingleKunRecommender(prio)
  def on(prio: Int) = new SingleOnRecommender(prio)
  def juku(prio: Int) = new SimpleJukugoRecommender(prio)
  def juman(prio: Int) = new JumanRecommender(prio)
}

class Recommender(uid: ObjectId) {
  import Recommender._
  import com.foursquare.rogue.LiftRogue._

  lazy val longjuku = List(kanji(100), kun(50), on(30))
  lazy val shortjuku = List(kun(100), on(75), juku(25))
  lazy val single = List(juku(100), juman(70), kun(50), on(50))
  lazy val rest = List(kun(70), on(70), juku(70), juman(50))


  def preprocess(cand: RecommendRequest): List[RecommendedSubresult] = {
    (cand.read, cand.writ) match {
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
        val ignored = RecommendationIgnore where (_.user eqs uid) select(_.jmdict) fetch()
        selector.flatMap(x => x.apply(ir)).flatMap(x => x.select(ignored))
    }
  }
}
