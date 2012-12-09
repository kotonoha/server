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

package ws.kotonoha.server.actors.dict

import akka.actor.Actor
import ws.kotonoha.server.actors.KotonohaMessage
import java.io.File
import net.liftweb.util.{Props => LP}
import ws.kotonoha.server.records.dictionary.ExampleSentenceRecord
import ws.kotonoha.server.dict.{TatoebaLink, TatoebaLinks}

/**
 * @author eiennohito
 * @since 29.04.12
 */

trait ExampleMessage extends KotonohaMessage
case class LoadExamples(data: List[ExampleIds]) extends ExampleMessage
case class TranslationsWithLangs(ids: List[Long], langs: List[String]) extends ExampleMessage

case class ExampleIds(jap: Long, other: List[TatoebaLink])
case class ExampleEntry(jap: ExampleSentenceRecord, other: List[ExampleSentenceRecord])
class ExampleActor extends Actor {

  val exampleSearcher = new TatoebaLinks(new File(LP.get("example.index").get))



  import com.foursquare.rogue.Rogue._

  override def postStop() {
    super.postStop()
    exampleSearcher.close()
  }

  protected def receive = {
    case LoadExamples(eids) =>  {
      val ids = (eids flatMap (ei => List(ei.jap) ++ ei.other.map(_.right))).distinct
      val recs = (ExampleSentenceRecord where (_.id in ids) fetch() map (r => r.id.is -> r)).toMap
      val objs = eids.flatMap( r => {
        val exr = recs.get(r.jap)
        val o = r.other.flatMap(t => recs.get(t.right))
        exr.map(e => ExampleEntry(e, o))
      })
      sender ! objs
    }
    case TranslationsWithLangs(ids, lang) => {
      sender ! ids.map { id => {
        val other = exampleSearcher.from(id).filter(t => lang.contains(t.rightLang))
        ExampleIds(id, other.toList)
      }}
    }
  }
}
