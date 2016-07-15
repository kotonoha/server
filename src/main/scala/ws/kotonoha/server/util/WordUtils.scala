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

package ws.kotonoha.server.util

import ws.kotonoha.server.records.dictionary.{JMDictMeaning, JMDictRecord}
import ws.kotonoha.server.japanese._
import xml.NodeSeq

object WordUtils {
   def processWord(writing:String, reading:Option[String]): Option[NodeSeq] = try {
     val meanings: List[JMDictMeaning] = JMDictRecord.query(writing, reading, 1).head.meaning.is
     val word_type = meanings.headOption.flatMap(_.info.is.headOption)
     val cobj = ConjObj(word_type.getOrElse("exp"), writing)
     val ns1 = cobj.masuForm.data map {c => <div>{c}</div> }
     val ns2 = cobj.teForm.data map {c => <div>{c}</div>}
     ns1 flatMap{s => ns2 map {t => s ++ t}}
   } catch { case _: Throwable => None }
}
