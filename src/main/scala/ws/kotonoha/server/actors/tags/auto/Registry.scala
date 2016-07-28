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

import akka.actor.{Actor, ActorLogging, Props}
import ws.kotonoha.server.actors.tags.TagMessage
import ws.kotonoha.server.actors.UserScopedActor

import concurrent.Future
import akka.util.Timeout
import com.google.inject.Inject
import ws.kotonoha.server.ioc.IocActors

/**
 * @author eiennohito
 * @since 20.01.13 
 */

//for manifest
import language.existentials

case class AutoTagger(name: String, wiki: Option[String], clz: Manifest[_])


object Registry {

  private def mf[T](implicit mf: Manifest[T]) = mf

  val taggers = List(
    AutoTagger("basic", None, mf[BasicTagger]),
    AutoTagger("jmdict", None, mf[JMDictTagger]),
    AutoTagger("yojijukugo", None, mf[YojijukugoTagger]),
    AutoTagger("kanjdic", None, mf[KanjidicTagger])
  )
}

case class PossibleTagRequest(writing: String, reading: Option[String]) extends TagMessage

case class PossibleTags(tags: Seq[String])

class WordAutoTagger @Inject() (
  ioc: IocActors
) extends UserScopedActor with ActorLogging {

  import akka.pattern.{ask, pipe}
  import concurrent.duration._

  lazy val children = {
    Registry.taggers.map {
      n => context.actorOf(ioc.props(n.clz), n.name)
    }
  }

  implicit val timeout: Timeout = 5 seconds

  def receive = {
    case r: PossibleTagRequest =>
      val futs = children.map(_ ? r).map(_.mapTo[PossibleTags])
      val f = Future.fold(futs)(List[String]())(_ ++ _.tags).map(l => PossibleTags(l.distinct))
      f pipeTo sender
  }
}
