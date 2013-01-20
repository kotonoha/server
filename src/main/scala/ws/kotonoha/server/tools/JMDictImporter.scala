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

package ws.kotonoha.server.tools

import java.io.FileInputStream
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.{JMString, JMDictMeaning, JMDictRecord}
import com.mongodb.BasicDBObject
import ws.kotonoha.akane.dict.jmdict.{JMDictParser, JMString => PJMS}

/**
 * @author eiennohito
 * @since 13.04.12
 */

object JMDictImporter {

  import resource._

  def jmstring(in: PJMS): JMString = {
    val jms = JMString.createRecord
    jms.priority(in.priority)
    jms.value(in.value)
    jms.info(in.info)
  }

  def main(args: Array[String]) = {
    val name = args(0)
    MongoDbInit.init()
    JMDictRecord.delete(new BasicDBObject()) //delete all
    for (input <- managed(new FileInputStream(name))) {
      val entries = JMDictParser.parse(input)
      val enrecs = entries map (e => {
        val rec = JMDictRecord.createRecord
        rec.id(e.id)
        rec.meaning(e.meaning.map(m => {
          val mn = JMDictMeaning.createRecord
          mn.info(m.info)
          mn.vals(m.vals)
        }))
        rec.reading(e.reading.map(jmstring(_)))
        rec.writing(e.writing.map(jmstring(_)))
      })
      //val data = entries.slice(0, 20).toArray
      enrecs.foreach(_.save)
    }
  }

}
