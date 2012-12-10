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

package ws.kotonoha.server.web.comet

import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.actors.lift.{ExecJs, DestroyActor, RegisterPerUserActor, AkkaInterop}
import com.weiglewilczek.slf4s.Logging
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 11.07.12
 */

case class ActorUser(user: ObjectId)

class GlobalActor extends NamedCometActor with AkkaInterop with Logging with ReleaseAkka {
  def render = defaultHtml

  var user = new ObjectId()

  override def localShutdown {
    if (user != 0L) {
      akkaServ ! DestroyActor(user)
    }
    super.localShutdown
  }

  override def lowPriority = {
    case ActorUser(u) => {
      if (user != u) {
        if (user != 0L) {
          akkaServ ! DestroyActor(user)
        }
        user = u
        akkaServ ! RegisterPerUserActor(user, sender)
      }
    }
    case ExecJs(js) => partialUpdate(js)
  }
}
