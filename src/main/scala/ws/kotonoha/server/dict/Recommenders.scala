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

import org.apache.lucene.search.BooleanClause.Occur
import ws.kotonoha.akane.conjuation.{AdjI, Verb}
import ws.kotonoha.akane.dic.jmdict.{JMDictUtil, JmdictEntry}
import ws.kotonoha.akane.unicode.KanaUtil
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.server.web.comet.Candidate

/**
 * @author eiennohito
 * @since 19.03.13 
 */

case class RecommendItem(id: Long, title: String, readings: Seq[String], writings: Seq[String], meanings: Seq[String])

case class RecommendedSubresult(item: JmdictEntry, title: String, prio: Int) {
  override def hashCode() = item.id.hashCode()

  override def equals(obj: Any) = obj match {
    case null => false
    case o: RecommendedSubresult => item.id.equals(o.item.id)
    case _ => false
  }

  def format(lngs: Set[String]) = {
    val wr = item.writings.map(_.content)
    val rd = item.readings.map(_.content)
    val mn = item.meanings.map { m =>
      val b = (m.pos ++ m.info).mkString("(", ", ", ") ")
      val e = m.content.filter(ls => lngs.contains(ls.lang)).map(_.str).mkString(", ")
      if (b.length > 3)
        b + e
      else e
    }
    RecommendItem(item.id, title, rd, wr, mn)
  }
}

class RecommendChunk(cand: Candidate, prio: Int, title: String, re: Boolean = false) {
  def select(jmdict: LuceneJmdict, ignore: Seq[Long]): Seq[RecommendedSubresult] = {
    val q = JmdictQuery(
      limit = 10,
      readings = cand.reading.map(r => JmdictQueryPart(r, Occur.MUST)).toSeq,
      other = cand.meaning.map(m => JmdictQueryPart(m, Occur.MUST)).toSeq,
      writings = Seq(JmdictQueryPart(cand.writing, Occur.MUST)),
      ignore = ignore,
      tags = Nil
    )

    val entries = jmdict.find(q)
    val x = cand.sortResults(entries.data, keep = 3)
    x.map(i => RecommendedSubresult(i, title, prio + JMDictUtil.calculatePriority(i.writings) * 1000))
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
      val kuns = ki.rmgroups.get.flatMap(_.kunyomi)
      val lit = ki.literal.get
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
      val kuns = ki.rmgroups.get.flatMap(_.onyomi)
      kuns.map { rd =>
        val have = in.reading.contains(rd)
        val c = Candidate(ki.literal.get, Some(rd), None)
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
      val kuns = ki.rmgroups.get.flatMap(_.onyomi)
      val lit = ki.literal.get
      kuns.flatMap { rd =>
        val kana = KanaUtil.kataToHira(rd)
        val c1 = Candidate(s"$lit.", Some(s"$kana*"), None)
        val c2 = Candidate(s".$lit", Some(s"*$kana"), None)
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
