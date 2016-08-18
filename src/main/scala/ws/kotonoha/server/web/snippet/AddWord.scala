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

import net.liftweb.http.S
import net.liftweb.common.Full
import ws.kotonoha.server.actors.lift.pertab.InsertNamedComet
import ws.kotonoha.server.util.unapply.XOid
import ws.kotonoha.server.web.comet.{Cleanup, PrepareWords, WordList}
import ws.kotonoha.server.records.UserRecord

/**
 * @author eiennohito
 * @since 17.03.12
 */

object AddFormActorSnippet extends InsertNamedComet {
  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass = "AddWordActor"

  override def name = UserRecord.currentUserId.map("add" + _).openOr(super.name)
}

object ApproveWordActorSnippet extends InsertNamedComet {

  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass = "ApproveWordActor"

  override def name = UserRecord.currentUserId.map("approve" + _).openOr(super.name)

  override def messages = {
    Cleanup :: (S.param("list") match {
      case Full(XOid(id)) => WordList(id) :: Nil
      case _ => PrepareWords :: Nil
    })
  }
}
