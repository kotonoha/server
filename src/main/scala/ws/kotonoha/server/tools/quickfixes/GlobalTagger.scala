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

package ws.kotonoha.server.tools.quickfixes

import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.actors.ioc.{ReleaseAkka, Akka}
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.actors.tags.auto.{PossibleTags, WordAutoTagger, PossibleTagRequest}
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.CreateActor
import akka.actor.{ActorRef, Props}
import concurrent.{Future, Await}
import ws.kotonoha.server.actors.tags.{AddTag, TagWord}
import collection.mutable.ListBuffer
import akka.util.Timeout

/**
 * @author eiennohito
 * @since 20.01.13 
 */

object GlobalTagger extends Akka with ReleaseAkka {

  import akka.pattern.{ask, pipe}
  import scala.concurrent.duration._

  implicit val timeout: Timeout = 1 minute
  implicit val ec = akkaServ.context

  def main(args: Array[String]) {
    MongoDbInit.init()
    val tagger =
      Await.result(
        akkaServ
          .userActorF(new ObjectId())
          .flatMap {
          _ ? CreateActor(Props[WordAutoTagger], "tagger")
        }
          .mapTo[ActorRef],
        1 minute)
    val words = WordRecord.findAll
    val buf = new ListBuffer[Future[Any]]()
    val total = words.length
    var cnt = 0
    words.foreach {
      w =>
        val w1 = w.writing.is.head
        val w2 = w.reading.is.headOption
        val req = PossibleTagRequest(w1, w2)
        val tagf = (tagger ? req).mapTo[PossibleTags].map(_.tags).map(x => x.map {
          AddTag(_)
        })
        val uid = w.user.is
        val f = akkaServ.userActorF(uid).flatMap {
          a =>
            tagf.flatMap {
              ops =>
                if (ops.length > 0)
                  a ? TagWord(w, ops)
                else tagf
            }
        }
        buf += f
        if (buf.length >= 100) {
          val fr = Future.sequence(buf.result())
          Await.ready(fr, 1 minute)
          buf.clear()
          cnt += 100
          println(s"done $cnt of $total")
        }
    }
    akkaServ.shutdown()
  }
}
