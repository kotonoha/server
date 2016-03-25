/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.web.comet

import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.actors.lift.{ToAkka, NgLiftActor}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.js.JsCmds._Noop
import net.liftweb.json.JsonAST.{JString, JField, JObject, JValue}
import net.liftweb.json.{DefaultFormats, Extraction}
import ws.kotonoha.server.records.{UserTagInfo, UserRecord}
import ws.kotonoha.server.actors.ForUser
import ws.kotonoha.server.actors.tags.{Taglist, TaglistRequest, UpdateTagPriority}
import com.typesafe.scalalogging.{StrictLogging => Logging}

/**
 * @author eiennohito
 * @since 16.02.13 
 */

case class DisplayingTagPriority(tag: String, usage: Long, priority: Int, limit: Option[Int])

case object PublishTags

class TagPriorityActor extends NamedCometActor with NgLiftActor with ReleaseAkka with Logging {

  val self = this

  val uid = UserRecord.currentId.openOrThrowException("This page shouldn't been displayed for not logged in user")

  def svcName = "tagSvc"

  implicit val formats = DefaultFormats

  import ws.kotonoha.server.util.KBsonDSL._

  override def receiveJson = {
    case x => self ! ProcessJson(x); _Noop
  }

  def saveData(value: JValue): Unit = {
    val data = Extraction.extract[List[DisplayingTagPriority]](value)
    val old = cachedTags.map {
      x => x.tag.is -> x
    } toMap

    data.foreach {
      dtp =>
        val obj = old(dtp.tag)
        if (obj.limit.is != dtp.limit || obj.priority.is != dtp.priority) {
          akkaServ ! ForUser(uid, UpdateTagPriority(dtp.tag, dtp.priority, dtp.limit))
        }
    }
  }

  override def render = {
    toAkka(ForUser(uid, TaglistRequest))
    super.render
  }

  def execute(cmd: String, dat: JValue): Unit = {
    cmd match {
      case "save" => saveData(dat)
    }
  }

  def processJson(value: JValue): Unit = {
    value match {
      case JObject(
      JField("cmd", JString(cmd)) ::
        JField("data", data) :: Nil
      ) => execute(cmd, data)
      case _ => logger.warn(s"invalid json $value")
    }
  }


  var cachedTags: List[UserTagInfo] = _

  def publishTags(tags: List[UserTagInfo]): Unit = {
    cachedTags = tags
    val data = tags map {
      t => DisplayingTagPriority(t.tag.is, t.usage.is, t.priority.is, t.limit.is)
    }
    val jv = Extraction.decompose(data)
    val msg = ("cmd" -> "tags") ~ ("data" -> jv)
    ngMessage(msg)
  }

  override def lowPriority = {
    case ProcessJson(x) => processJson(x)
    case Taglist(tags) => publishTags(tags)
  }
}
