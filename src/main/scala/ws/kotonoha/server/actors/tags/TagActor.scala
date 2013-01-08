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
import ws.kotonoha.server.actors.UserScopedActor
import akka.dispatch.Await
import akka.util.Timeout
import akka.util.duration._
import net.liftweb.json.JsonAST._
import collection.mutable.ListBuffer
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST.JString
import net.liftweb.json.JsonAST.JArray

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class TagActor extends UserScopedActor {
  implicit val timeout: Timeout = 10 seconds

  lazy val svc = Await.result((services ? ServiceActor).mapTo[ActorRef], 10 seconds)

  protected def receive = {
    case Nil => //
  }
}

case class TagData(tags: List[String], ops: List[TagOp])

sealed trait TagOp {
  def transform(tags: List[String]): List[String]
}
case class AddTag(tag: String) extends TagOp {
  def transform(tags: List[String]) = tags ++ List(tag)
}

case class RemoveTag(tag: String) extends TagOp {
  def transform(tags: List[String]) = tags.filterNot(_.equals(tag))
}

case class RenameTag(from: String, to: String) extends TagOp {
  def transform(tags: List[String]) = tags.map {
    case s if s.equals(from) => to
    case s => s
  }
}


object TagParser {

  def parseObj(jv: JValue): AnyRef = jv match {
    case JString(s) => s
    case JObject(JField("add", JString(s)) :: _) => AddTag(s)
    case JObject(JField("remove", JString(s)) :: _) => RemoveTag(s)
    case JObject(JField("rename", JString(from)) :: JField("to", JString(to)) :: _) => RenameTag(from, to)
    case JObject(JField("to", JString(from)) :: JField("rename", JString(to)) :: _) => RenameTag(from, to)
    case _ => TagParser
  }

  def parseArr(vals: List[JValue]): TagData = {
    val tags = new ListBuffer[String]
    val ops = new ListBuffer[TagOp]
    vals foreach {
      parseObj(_) match {
        case s: String => tags += s
        case o: TagOp => ops += o
        case _ => //
      }
    }
    TagData(tags.result(), ops.result())
  }

  def parseOps(jv: JValue): TagData = jv match {
    case JArray(arr) => parseArr(arr)
    case _ => TagData(Nil, Nil)
  }

  def performTransform(jv: JValue) = {
    val data = parseOps(jv)
    data.ops.foldLeft(data.tags){ (t, o) => o.transform(t) }
  }
}

trait Taggable {
  def curTags: List[String]
  def writeTags(tags: List[String])
}
