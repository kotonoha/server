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

import java.util.function.Function

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.{Inject, Provider, Provides, Singleton}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import ws.kotonoha.akane.dic.jmdict.JmdictEntry
import ws.kotonoha.akane.dic.lucene.jmdict.{JmdictSearchResults, LuceneJmdict}
import ws.kotonoha.akane.dic.lucene.jmdict.{JmdictQuery, JmdictQueryPart, JmdictSearchResults}
import ws.kotonoha.examples.ExampleClient
import ws.kotonoha.examples.api.{ExamplePack, ExamplePackRequest}
import ws.kotonoha.server.grpc.GrpcClients
import ws.kotonoha.server.records.WordRecord

import scala.concurrent.{ExecutionContextExecutor, Future}

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

  def similarEntriesImpl(dic: Seq[JmdictEntry], rec: WordRecordWrapper): Seq[JmdictEntry]  = {
    if (dic.isEmpty) return Nil
    val scores = dic.map { d =>
      d -> score(d, rec)
    }
    scores.sortBy(-_._2).map(_._1)
  }

  def bestMatchImpl(dic: Seq[JmdictEntry], rec: WordRecordWrapper): Option[JmdictEntry] = {
    if (dic.isEmpty) None
    similarEntriesImpl(dic, rec).headOption
  }

  def entryForWord(rec: WordRecord) = {
    val result: JmdictSearchResults = rawSimilar(rec)
    bestMatchImpl(result.data, new WordRecordWrapper(rec))
  }

  def nsimilarForWord(rec: WordRecord, n: Int = 10) = {
    val result: JmdictSearchResults = rawSimilar(rec, n)
    similarEntriesImpl(result.data, new WordRecordWrapper(rec))
  }

  def rawSimilar(rec: WordRecord, n: Int = 10): JmdictSearchResults = {
    val q = JmdictQuery(
      limit = n,
      readings = rec.reading.get.map(r => JmdictQueryPart(term = r)),
      writings = rec.writing.get.map(r => JmdictQueryPart(term = r))
    )
    jmd.find(q)
  }
}

@Singleton
class ExampleCacher @Inject() (
  ecl: Provider[ExampleClient],
  ece: ExecutionContextExecutor
) {
  private[this] val cache = {
    Caffeine.newBuilder()
        .maximumSize(10000)
        .executor(ece)
        .build[ExamplePackRequest, Future[ExamplePack]]()
  }

  private[this] val genericInt = new Function[ExamplePackRequest, Future[ExamplePack]] {
    override def apply(t: ExamplePackRequest) = {
      val f = ecl.get().generic(t)
      f.onFailure {
        case t => cache.invalidate(t)
      }(ece)
      f
    }
  }

  def generic(req: ExamplePackRequest): Future[ExamplePack] = {
    cache.get(req, genericInt)
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
