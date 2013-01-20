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

package ws.kotonoha.server.actors.tags

import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import ws.kotonoha.server.actors.KotonohaActor
import ws.kotonoha.server.records.{TagInfo, WordTagInfo, TagAlias}
import org.bson.types.ObjectId
import net.liftweb.common.Full
import com.mongodb.casbah.WriteConcern

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class TagService extends KotonohaActor {

  import com.foursquare.rogue.LiftRogue._

  val sid = new ObjectId(0, 0, 0)

  def addAlias(from: String, to: String): Unit = {
    val alias = TagAlias.createRecord
    alias.alias(from)
    alias.tag(to)
    alias.save
    Tags.updateAliases(Tags.aliases + (from -> to))
  }

  def removeTagAlias(alias: String): Unit = {
    val aobj = TagAlias.find("alias", alias)
    aobj match {
      case Full(x) => x.delete_!
      case _ => //
    }
    Tags.updateAliases(Tags.aliases - alias)
  }

  def handleGlobalUsage(tag: String, num: Int): Unit = {
    TagInfo where (_.tag eqs tag) findAndModify (_.usage inc (num)) updateOne (false) match {
      case None if num > 0 =>
        val rec = TagInfo.createRecord
        rec.tag(tag)
        rec.usage(num)
        rec.save(WriteConcern.Safe)
      case _ =>
    }
  }

  override def receive = {
    case ServiceActor => sender ! self
    case AddTagAlias(from, to) => addAlias(from, to)
    case RemoveTagAlias(from) => removeTagAlias(from)
    case GlobalTagWritingStat(writ, tag, cnt) => Tags.handleWritingStat(writ, tag, cnt)
    case GlobalUsage(tag, cnt) => handleGlobalUsage(tag, cnt)
  }
}

object Tags {

  import com.foursquare.rogue.LiftRogue._

  private[tags] def updateAliases(m: Map[String, String]): Unit = {
    aliases_ = m
  }

  @volatile private var aliases_ = {
    TagAlias.fetch().map(a => a.alias.is -> a.tag.is).toMap.withDefault(x => x)
  }

  def aliases = aliases_

  def handleWritingStat(writ: String, tag: String, cnt: Int): Unit = {
    val q = WordTagInfo where (_.word eqs writ) and (_.tag eqs tag)
    q findAndModify (_.usage inc cnt) updateOne (false) match {
      case None if cnt > 0 => {
        val rec = WordTagInfo.createRecord
        rec.tag(tag)
        rec.usage(cnt)
        rec.word(writ)
        rec.save(WriteConcern.Safe)
      }
      case _ =>
    }
  }


}
