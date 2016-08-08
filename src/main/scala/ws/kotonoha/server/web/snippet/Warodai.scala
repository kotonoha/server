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

package ws.kotonoha.server.web.snippet

import com.typesafe.scalalogging.StrictLogging
import ws.kotonoha.server.records.dictionary.WarodaiRecord

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.{Box, Full}
import ws.kotonoha.server.dict.WarodaiBodyParser

import scala.util.parsing.input.CharSequenceReader


/**
 * @author eiennohito
 * @since 12.04.12
 */

class Warodai extends StrictLogging {
  import ws.kotonoha.server.web.lift.Binders._

  private val query = S.param("query").openOr("宅")

  def parse(sb: Box[String]): NodeSeq = sb match {
    case Full(s) => {
      val pr = WarodaiBodyParser.body(new CharSequenceReader(s))
      if (pr.successful) {
        pr.get.toNodeSeq
      } else {
        logger.warn("couldn't parse " + pr)
        <span></span>
      }
    }
    case _ => <span></span>
  }

  def fld(in: NodeSeq): NodeSeq = {
    val fn = "@query [value]" #> query
    fn(in)
  }

  def list(in: NodeSeq): NodeSeq = {
    val results = WarodaiRecord.query(query, None, 50)
    val fn = bseq(results) { o =>
      ".writing *" #> o.writings.get.mkString(", ") &
      ".reading *" #> o.readings.get.mkString(", ") &
      ".information *" #> parse(o.body.valueBox)
    }
    fn(in)
  }
}
