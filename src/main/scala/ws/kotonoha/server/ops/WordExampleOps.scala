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

import com.google.inject.{Inject, Provider, Provides}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import ws.kotonoha.akane.dic.jmdict.JmdictEntry
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.examples.ExampleClient
import ws.kotonoha.examples.api.{ExamplePackRequest, ExampleQuery, ExampleTag}
import ws.kotonoha.server.grpc.GrpcClients
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordRecord

/**
  * @author eiennohito
  * @since 2016/08/16
  */

private[ops] class WordRecordWrapper(val rec: WordRecord) {
  val writings = rec.writing.value.toSet
  val readings = rec.reading.value.toSet
}

class WordJmdictOps @Inject() (
  jmd: LuceneJmdict
) {

  def score(d: JmdictEntry, rec: WordRecordWrapper): Double = {
    var score = 0.0
    score += d.writings.count(k => rec.writings.contains(k.content)) * 2
    score += d.readings.count(r => rec.readings.contains(r.content))
    score
  }

  def bestMatch(dic: Seq[JmdictEntry], rec: WordRecordWrapper): Option[JmdictEntry] = {
    if (dic.isEmpty) return None
    val scores = dic.map { d =>
      d -> score(d, rec)
    }
    Some(scores.maxBy(_._2)._1)
  }

  def entryForWord(rec: WordRecord) = {
    val q = JmdictQuery(
      limit = 10,
      readings = rec.reading.get.map(r => JmdictQueryPart(term = r)),
      writings = rec.writing.get.map(r => JmdictQueryPart(term = r))
    )
    val result = jmd.find(q)
    bestMatch(result.data, new WordRecordWrapper(rec))
  }
}

class WordExampleOps @Inject() (
  rm: RMData,
  word: WordOps,
  esvc: Provider[ExampleClient]
) {

  def unknownExReq(rec: WordRecord): ExamplePackRequest = {
    val q = ExampleQuery(rec.writing.value.head, rec.reading.value.head)

    ExamplePackRequest(
      tags = Seq(ExampleTag.Unknown),
      candidates = Seq(q)
    )
  }

  def acquireExamples(w: WordRecord) = {
    val req = unknownExReq(w)
    val res = esvc.get().generic(req)
    res
  }



}

case class ExampleServiceConfig(uri: String)

class RepExampleModule extends ScalaModule {
  override def configure() = {}

  @Provides
  def cfg(cf: Config): ExampleServiceConfig = {
    val uri = cf.getString("examples.uri")
    ExampleServiceConfig(uri)
  }

  @Provides
  def exClient(
    cfg: ExampleServiceConfig,
    gc: GrpcClients
  ): ExampleClient = {
    gc.clientTo(cfg.uri).service(ExampleClient)
  }
}
