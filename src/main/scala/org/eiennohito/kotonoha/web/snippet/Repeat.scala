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

import org.eiennohito.kotonoha.actors.ioc.{ReleaseAkka, Akka}
import com.fmpwizard.cometactor.pertab.namedactor.InsertNamedComet
import org.eiennohito.kotonoha.records.UserRecord
import net.liftweb.util.Helpers
import org.eiennohito.kotonoha.web.comet.{RepeatUser}

/**
 * @author eiennohito
 * @since 21.05.12
 */

object Repeat extends Akka with ReleaseAkka {

}

object RepeatActorSnippet extends InsertNamedComet {
  def cometClass = "RepeatActor"

  override def messages = RepeatUser(UserRecord.currentId.getOrElse(-1L)) :: Nil

  override def name = UserRecord.currentUserId openOr(Helpers.nextFuncName)
}
