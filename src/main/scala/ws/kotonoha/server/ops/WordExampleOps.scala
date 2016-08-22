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
import ws.kotonoha.akane.dic.jmdict.{JMDictUtil, JmdictEntry, JmdictTag}
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, JmdictSearchResults, LuceneJmdict}
import ws.kotonoha.examples.ExampleClient
import ws.kotonoha.examples.api.{ExamplePack, ExamplePackRequest, ExampleQuery, ExampleTag}
import ws.kotonoha.server.grpc.GrpcClients
import ws.kotonoha.server.records.WordRecord

import scala.collection.mutable.ArrayBuffer
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

class WordExampleOps @Inject() (
  ec: ExampleCacher,
  jmd: LuceneJmdict,
  wjo: WordJmdictOps
) {
  def exampleRequest(w: WordRecord): ExamplePackRequest = {
    w.jmdictLink.get.flatMap(jmd.byId).orElse(wjo.entryForWord(w)) match {
      case Some(je) => WordExampleOps.jmdictReq(je)
      case None => WordExampleOps.wordRecordReq(w)
    }
  }

  def acquireExamples(w: WordRecord): Future[ExamplePack] = {
    val req = exampleRequest(w)
    val res = ec.generic(req)
    res
  }
}

object WordExampleOps {
  def wordRecordReq(rec: WordRecord): ExamplePackRequest = {
    val q = ExampleQuery(rec.writing.value.head, rec.reading.value.head)

    val tgs = rec.tags.get match {
      case t if t.contains("noun") && t.contains("vs") => ExampleTag.SuruVerb
      case t if t.contains("noun") => ExampleTag.Noun
      case t if t.contains("verb") => ExampleTag.Verb
      case t if t.contains("adj") => ExampleTag.Adjective
      case t if t.contains("adv") => ExampleTag.Adverb
      case _ => ExampleTag.Unknown
    }

    ExamplePackRequest(
      tags = Seq(tgs),
      candidates = Seq(q),
      limit = 15
    )
  }


  def jmdictReq(e: JmdictEntry) = {
    val qs = e.readings.flatMap { r =>
      val matchingWrs = if (r.restr.isEmpty) {
        e.writings
      } else {
        r.restr.map(e.writings.apply)
      }
      matchingWrs.map { w =>
        ExampleQuery(w.content, r.content) -> (JMDictUtil.calculatePriority(r) + JMDictUtil.calculatePriority(w))
      }
    }

    val qentries = qs.sortBy(-_._2).map(_._1)
    val qtags = new ArrayBuffer[ExampleTag]()

    val allTags = e.meanings.flatMap(_.pos).toSet

    if (allTags.contains(JmdictTag.vs)) {
      qtags += ExampleTag.SuruVerb
    }

    if (allTags.contains(JmdictTag.n)) {
      qtags += ExampleTag.Noun
    }

    if (allTags.intersect(JMDictUtil.verbTags).nonEmpty) {
      qtags += ExampleTag.Verb
    }

    if (allTags.intersect(JMDictUtil.adjTags).nonEmpty) {
      qtags += ExampleTag.Adjective
    }

    if (allTags.intersect(JMDictUtil.advTags).nonEmpty) {
      qtags += ExampleTag.Adverb
    }

    if (allTags.isEmpty) {
      qtags += ExampleTag.Unknown
    }

    ExamplePackRequest(
      qtags, qentries, limit = 15
    )
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
