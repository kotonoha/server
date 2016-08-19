/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.examples.api

import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.json.JsonAST.{JString, JValue}
import ws.kotonoha.akane.dic.jmdict._
import ws.kotonoha.lift.json.{JFormat, JLCaseClass}

/**
  * @author eiennohito
  * @since 2016/08/19
  */
object JmdictJson {
  implicit val jmdTagFmt: JFormat[JmdictTag] = new JFormat[JmdictTag] {
    override def write(o: JmdictTag): JValue = {
      val repr = JmdictTagMap.tagInfo(o.value).repr
      JString(repr)
    }
    override def read(v: JValue): Box[JmdictTag] = v match {
      case JString(s) =>
        val item = JmdictTagMap.tagMap.get(s)
        item match {
          case Some(tg) => Full(tg)
          case None => Failure(s"invalid jmdict tag $s")
        }
      case _ => Failure(s"JMDict tag should be a string, was $v")
    }
  }

  implicit val prioFmt: JFormat[Priority] = new JFormat[Priority] {
    private val byName = Priority.values.map(v => v.name -> v).toMap

    override def write(o: Priority): JValue = JString(o.name)
    override def read(v: JValue): Box[Priority] = v match {
      case JString(s) =>
        val item = byName.get(s)
        item match {
          case Some(p) => Full(p)
          case None => Failure(s"invalid jmdict priority $s")
        }
      case _ => Failure(s"JMDict priority should be a string, was $v")
    }
  }

  implicit val lstrFormat = JLCaseClass.format[LocalizedString]
  implicit val xrefFmt = JLCaseClass.format[CrossReference]
  implicit val srcInfoFmt = JLCaseClass.format[SourceInfo]
  implicit val rinfoFmt = JLCaseClass.format[ReadingInfo]
  implicit val kinfoFmt = JLCaseClass.format[KanjiInfo]
  implicit val minfoFmt = JLCaseClass.format[MeaningInfo]
  implicit val eiformat = JLCaseClass.format[EntryInfo]
  implicit val jmEntryFmt = JLCaseClass.format[JmdictEntry]

  implicit val seqjEmtryFmt = implicitly[JFormat[Seq[JmdictEntry]]]
}
