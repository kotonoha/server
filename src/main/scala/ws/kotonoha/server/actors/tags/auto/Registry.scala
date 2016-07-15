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

package ws.kotonoha.server.actors.tags.auto

import akka.actor.{ActorLogging, Props}
import ws.kotonoha.server.actors.tags.TagMessage
import ws.kotonoha.server.actors.UserScopedActor
import concurrent.Future
import akka.util.Timeout

/**
 * @author eiennohito
 * @since 20.01.13 
 */

case class AutoTagger(name: String, wiki: Option[String], props: Props)

object Registry {
  val taggers = List(
    AutoTagger("basic", None, Props[BasicTagger]),
    AutoTagger("jmdict", None, Props[JMDictTagger]),
    AutoTagger("yojijukugo", None, Props[YojijukugoTagger]),
    AutoTagger("kanjdic", None, Props[KanjidicTagger])
  )
}

case class PossibleTagRequest(writing: String, reading: Option[String]) extends TagMessage

case class PossibleTags(tags: List[String])

class WordAutoTagger extends UserScopedActor with ActorLogging {

  import akka.pattern.{ask, pipe}
  import concurrent.duration._

  lazy val children = {
    Registry.taggers.map {
      n =>
        context.actorOf(n.props, n.name)
    }
  }

  implicit val timeout: Timeout = 5 seconds

  def receive = {
    case r: PossibleTagRequest => {
      val futs = children.map(_ ? r).map(_.mapTo[PossibleTags])
      val f = Future.fold(futs)(List[String]())(_ ++ _.tags).map(l => PossibleTags(l.distinct))
      f pipeTo sender
    }
  }
}
