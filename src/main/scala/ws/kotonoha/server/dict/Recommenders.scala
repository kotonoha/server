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

import ws.kotonoha.server.web.comet.Candidate
import ws.kotonoha.akane.conjuation.{AdjI, Verb}
import net.liftweb.json.JsonAST.{JObject, JValue}
import ws.kotonoha.server.records.dictionary.JMDictRecord
import net.liftweb.mongodb.Limit

/**
 * @author eiennohito
 * @since 19.03.13 
 */

case class RecommendItem(id: Long, title: String, readings: List[String], writings: List[String], meanings: List[String])

case class RecommendedSubresult(item: JMDictRecord, title: String, prio: Int) {
  override def hashCode() = item.id.is.hashCode()

  override def equals(obj: Any) = obj match {
    case null => false
    case o: RecommendedSubresult => item.id.is.equals(o.item.id.is)
    case _ => false
  }

  def format(lngs: Set[String]) = {
    val wr = item.writing.is.map(_.value.is)
    val rd = item.reading.is.map(_.value.is)
    val mn = item.meaning.is.map { m =>
      val b = m.info.is.mkString("(", ", ", ") ")
      val e = m.vals.is.filter(ls => lngs.contains(ls.loc)).map(_.str).mkString(", ")
      b + e
    }
    RecommendItem(item.id.is, title, rd, wr, mn)
  }
}

class RecommendChunk (cand: Candidate, prio: Int, title: String, re: Boolean = false) {
  import ws.kotonoha.server.util.KBsonDSL._

  def select(ignore: List[Long]) = {
    val q: JObject = cand.toQuery(re) ~ ("_id" -> ("$nin" -> ignore))
    val items = JMDictRecord.findAll(q, Limit(10))
    val x = JMDictRecord.sorted(items, cand).take(3)
    x.map(i =>
      RecommendedSubresult(i, title, prio + JMDictRecord.calculatePriority(i.writing.is) * 1000))
  }
}

trait RecommenderBlock {
  def apply(in: DoRecommend): List[RecommendChunk]
}

class KanjiRecommender(prio: Int) extends RecommenderBlock {
  def apply(in: DoRecommend) = {
    val len = in.writing.length
    (2 until len).flatMap { cnt =>
      in.writing.sliding(cnt)
    }.map(w => new RecommendChunk(Candidate(w, None, None), prio, "kanji")).toList
  }
}

class SingleKunRecommender(prio: Int) extends RecommenderBlock {

  def hasPartInReading(read: String, part: String) = {
    val data = part.last match {
      case 'る' => Verb.godan(part).masuStem :: Verb.ichidan(part).masuStem :: Nil
      case 'い' => AdjI(part).stem :: Nil
      case _ => Verb.godan(part).masuStem :: Nil
    }
    val trials = part :: data.flatMap(_.render)
    trials.exists(read.contains(_))
  }

  def apply(in: DoRecommend) = {
    val ks = in.kanji.flatMap(x => in.kinfo.get(x))
    ks.flatMap { ki =>
      val kuns = ki.rmgroups.is.flatMap(_.kunyomi)
      val lit = ki.literal.is
      kuns.map { rd =>
        val i = rd.indexOf('.')
        if (i == -1) {
          val c = Candidate(lit, Some(rd), None)
          new RecommendChunk(c, prio, "kun")
        } else {
          val wr = s"$lit${rd.substring(i + 1)}"
          val crd = rd.replace(".", "")
          val pri = if (hasPartInReading(in.reading, crd)) prio + 10 else prio
          val c = Candidate(wr, Some(crd), None)
          new RecommendChunk(c, pri, "kun")
        }
      }
    }
  }
}

class SingleOnRecommender(prio: Int) extends RecommenderBlock {
  def apply(in: DoRecommend) = {
    val ks = in.kanji.flatMap(x => in.kinfo.get(x))
    ks.flatMap { ki =>
      val kuns = ki.rmgroups.is.flatMap(_.onyomi)
      kuns.map { rd =>
        val have = in.reading.contains(rd)
        val c = Candidate(ki.literal.is, Some(rd), None)
        val p = if (have) prio + 5 else prio
        new RecommendChunk(c, p, "on")
      }
    }
  }
}

class SimpleJukugoRecommender(prio: Int) extends RecommenderBlock {
  def apply(in: DoRecommend) = {
    val ks = in.kanji.flatMap(x => in.kinfo.get(x))
    ks.flatMap { ki =>
      val kuns = ki.rmgroups.is.flatMap(_.onyomi)
      val lit = ki.literal.is
      kuns.flatMap { rd =>
        val c1 = Candidate(s"$lit.", Some(s"$rd"), None)
        val c2 = Candidate(s".$lit", Some(s".*$rd"), None)
        List(new RecommendChunk(c1, prio, "juku"), new RecommendChunk(c2, prio, "juku"))
      }
    }
  }
}

class JumanRecommender(prio: Int) extends RecommenderBlock {
  def apply(in: DoRecommend) = {
    val tags = in.juman.flatMap(_.tags)
    tags.flatMap { t =>
      t.value.split("/") match {
        case Array(w, r) =>
          val c = Candidate(w, Some(r), None)
          val tg = s"${t.tag}-${t.kind}"
          new RecommendChunk(c, prio, tg) :: Nil
        case _ => Nil
      }
    }
  }
}
