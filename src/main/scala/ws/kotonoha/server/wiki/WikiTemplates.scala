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

package ws.kotonoha.server.wiki

import scala.xml.{Group, Node}
import ws.kotonoha.server.wiki.template.{AddWordBtn, AddWord}

/**
 * @author eiennohito
 * @since 20.04.13 
 */
object WikiTemplates {
  def apply(invocation: String): Node = {
    val ns = invocation.split(":", 2) match {
      case Array(template) => tempaltes.get(template).map(t => t(""))
      case Array(template, args) => tempaltes.get(template).map(t => t(args))
    }
    Group(ns.getOrElse(Nil))
  }

  val tempaltes = Map(
    "addword" -> AddWord,
    "addwordbtn" -> AddWordBtn
  )
}
