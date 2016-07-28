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

import ws.kotonoha.server.actors.KotonohaMessage
import ws.kotonoha.server.records.{UserTagInfo, WordRecord}

/**
 * @author eiennohito
 * @since 07.01.13 
 */

trait TagMessage extends KotonohaMessage

//peruser messages
case class TagWord(rec: WordRecord, ops: Seq[TagOp]) extends TagMessage

//service messages
case object ServiceActor extends TagMessage

case class GlobalTagWritingStat(writ: String, tag: String, cnt: Int) extends TagMessage

case class GlobalUsage(tag: String, cnt: Int) extends TagMessage

case class AddTagAlias(from: String, to: String) extends TagMessage

case class RemoveTagAlias(from: String) extends TagMessage

case class UpdateTagPriority(tag: String, prio: Int, limit: Option[Int]) extends TagMessage

case class CalculatePriority(tags: List[String]) extends TagMessage

case class Priority(prio: Int)

case object TaglistRequest extends TagMessage

case class Taglist(info: List[UserTagInfo])
