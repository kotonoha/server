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

package ws.kotonoha.server.web.rest

import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.{JsonResponse, BadResponse, InMemoryResponse, ForbiddenResponse}
import net.liftweb.common.{Empty, Failure, Full}
import ws.kotonoha.akane.statistics.UniqueWordsExtractor
import ws.kotonoha.server.actors.interop.{KnpRequest, JumanActor}
import akka.actor.ActorRef
import scala.concurrent.{Future, Await}
import concurrent.duration._
import ws.kotonoha.akane.parser.{AozoraStringInput, AozoraParser}
import ws.kotonoha.akane.juman.JumanDaihyou
import ws.kotonoha.server.japanese.Stopwords
import ws.kotonoha.akane.pipe.knp.KnpNode
import net.liftweb.json.{DefaultFormats, Extraction}

/**
 * @author eiennohito
 * @since 20.08.12
 */

object Juman extends KotonohaRest with ReleaseAkka {

  def wer = {
    val af = (akkaServ ? JumanActor).mapTo[ActorRef]
    af map { new UniqueWordsExtractor(_, akkaServ.system.dispatcher) }
  }

  serve ("api" / "juman" prefix {
    case "unique_words" :: Nil Post req => {
      val limit = 2000
      val okay = req.body map {_.length < limit} openOr(false)
      if (!okay) {
        ForbiddenResponse("length below limit or no body")
      } else {
        val data = new String(req.body.get, "utf-8")
        async(wer) {
          wr => {
            val parser = new AozoraParser(new AozoraStringInput(data))
            val fut = wr.uniqueWords(parser, Stopwords.stopwords)
            val f = fut map {
              _.foldLeft(new StringBuilder) {
                case (sb, it) => it match {
                  case JumanDaihyou(s, "") => sb.append(s).append("\n")
                  case JumanDaihyou(s, r) => sb.append(s).append("|").append(r).append("\n")
                }
              } toString()
            }
            f.map {
              s => Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))
            } recover {
              case e => Failure("error with stuff", Full(e), Empty)
            }
          }
        }
      }
    }
  })

  serve("api" / "knp" prefix {
    case "analyze" :: Nil Get req =>
      val request = req.param("q")
      request match {
        case Empty => Full(BadResponse())
        case Full(x) if x.length > 200 => Full(BadResponse())
        case Full(x) =>
          val req = (akkaServ ? KnpRequest(x)).mapTo[KnpNode]
          async(req) { x =>
            val jvalue = Extraction.decompose(x)(DefaultFormats)
            Future.successful(Full(JsonResponse(jvalue)))
          }
      }
  })
}
