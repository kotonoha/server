
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

package ws.kotonoha.server.web.comet

import net.liftweb.http.CometActor
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.json.JsonAST.{JString, JValue}
import ws.kotonoha.server.actors.interop.{KnpResponse, KnpRequest}
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.json.{DefaultFormats, Extraction}

/**
 * @author eiennohito
 * @since 2013-09-05
 */
class KnpActor extends CometActor with NgLiftActor with AkkaInterop with ReleaseAkka with Logging {
  def svcName = "knpService"
  val self = this

  override def receiveJson = {
    case x =>
      self ! ProcessJson(x)
  }

  def processJson(value: JValue): Unit = {
    val cmd = value.findField(_.name == "cmd").map(_.value)
    cmd match {
      case Some(JString("analyze")) =>
        val content = value \\ "content"
        content.obj.head.value match {
          case JString(x) =>
            toAkka(KnpRequest(x))
          case _ =>
            logger.warn("unacceptable command for knp: " + value)
        }
      case _ =>
        logger.warn("unacceptable command for knp: " + value)
    }
  }

  def sendResponse(response: KnpResponse): Unit = {
    import ws.kotonoha.server.util.KBsonDSL._
    val jv = Extraction.decompose(response)(DefaultFormats)
    val outjv = ("cmd" -> "results") ~ ("content" -> jv)
    ngMessage(outjv)
  }

  override def lowPriority = {
    case ProcessJson(x) => processJson(x)
    case r: KnpResponse => sendResponse(r)
  }
}
