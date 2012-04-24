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

package org.eiennohito.kotonoha.web.snippet

import org.eiennohito.kotonoha.records.dictionary.WarodaiRecord
import net.liftweb.mongodb.Limit
import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.json._
import net.liftweb.json.JsonAST.JObject
import com.weiglewilczek.slf4s.Logging
import net.liftweb.common.{Full, Box}
import org.eiennohito.kotonoha.dict.WarodaiBodyParser
import util.parsing.input.CharSequenceReader

/**
 * @author eiennohito
 * @since 12.04.12
 */

object Warodai extends Logging {
  import org.eiennohito.kotonoha.util.KBsonDSL._
  import net.liftweb.util.Helpers._

  def parse(sb: Box[String]): NodeSeq = sb match {
    case Full(s) => {
      val pr = WarodaiBodyParser.body(new CharSequenceReader(s))
      if (pr.successful) {
        pr.get.toNodeSeq
      } else {
        logger.warn("couldn,t parse " + pr)
        <span></span>
      }
    }
    case _ => <span></span>
  }

  def fld(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    bind("frm", in, AttrBindParam("value", q, "value"))
  }

  def list(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    val re = "^" + q
    val inner = "$regex" -> re
    val query: JObject = "$or" -> List(("readings" -> inner), ("writings" -> inner))
    val jse = compact(render(query))
    logger.debug(jse)
    val objs = WarodaiRecord.findAll(query, Limit(50))
    objs flatMap { o =>
      bind("we", in,
        "writing" -> o.writings.is.mkString(", "),
        "reading" -> o.readings.is.mkString(", "),
        "body" -> parse(o.body.valueBox))
    }
  }
}
