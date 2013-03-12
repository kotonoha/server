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

package ws.kotonoha.server.records.dictionary

import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.field.{MongoListField, MongoCaseClassListField, BsonRecordListField, LongPk}
import ws.kotonoha.server.mongodb.DictDatabase
import net.liftweb.json.JsonAST.JObject
import net.liftweb.mongodb.Limit
import ws.kotonoha.akane.dict.jmdict.{Priority, LocString}
import ws.kotonoha.server.web.comet.Candidate
import ws.kotonoha.server.math.MathUtil

/**
 * @author eiennohito
 * @since 14.04.12
 */

class JMDictMeaning extends BsonRecord[JMDictMeaning] {
  def meta = JMDictMeaning

  object info extends MongoListField[JMDictMeaning, String](this)

  object vals extends MongoCaseClassListField[JMDictMeaning, LocString](this)

}

object JMDictMeaning extends JMDictMeaning with BsonMetaRecord[JMDictMeaning]

class JMString extends BsonRecord[JMString] {
  def meta = JMString

  object priority extends MongoCaseClassListField[JMString, Priority](this)

  object value extends StringField(this, 500)

  object info extends MongoListField[JMString, String](this)

}

object JMString extends JMString with BsonMetaRecord[JMString]

class JMDictRecord private() extends MongoRecord[JMDictRecord] with LongPk[JMDictRecord] {
  def meta = JMDictRecord

  object reading extends BsonRecordListField(this, JMString)

  object writing extends BsonRecordListField(this, JMString)

  object meaning extends BsonRecordListField(this, JMDictMeaning)
}

object JMDictRecord extends JMDictRecord with MongoMetaRecord[JMDictRecord] with DictDatabase {
  def query(w: String, rd: Option[String], max: Int, re: Boolean = false) = {
    val c = Candidate(w, rd, None)
    forCandidate(c, max, re)
  }

  def sorted(recs: List[JMDictRecord], c: Candidate = null) = {
    if (c != null && (c.isOnlyKana || c.sameWR)) {
      def penalty(r: JMDictRecord) = {
        if (r.writing.is.length > 0) 10 else 0
      }
      recs.sortBy(r => -calculatePriority(r.writing.is) + penalty(r))
    } else {
      recs.sortBy(r => -calculatePriority(r.writing.is))
    }
  }

  def forCandidate(cand: Candidate, limit: Int, re: Boolean = false) = {
    val jv = cand.toQuery(re)
    sorted(JMDictRecord.findAll(jv, Limit(limit)), cand)
  }

  def calculatePriority(strs: List[JMString]): Int = {
    val x = strs.flatMap {
      _.priority.is.map {
        _.value
      }
    }.map {
      case "news1" => 2
      case "news2" => 1
      case "ichi1" => 2
      case "ichi2" => 1
      case "spec1" => 2
      case "spec2" => 1
      case "gai1" => 2
      case "gai2" => 1
      case s if s.startsWith("nf") => 2 - (s.substring(2).toInt / 24)
      case _ => 0
    }

    val cnt = x.length
    if (cnt == 0) 0
    else {
      val sum = x.fold(0)(_ + _)
      Math.round(sum.toDouble / cnt).toInt
    }
  }
}
