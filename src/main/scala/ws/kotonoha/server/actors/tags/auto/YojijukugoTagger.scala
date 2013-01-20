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

package ws.kotonoha.server.actors.tags.auto

import ws.kotonoha.server.actors.UserScopedActor
import scalax.io.Codec

/**
 * @author eiennohito
 * @since 20.01.13 
 */

object Yojijukugo {
  val data = {
    import scalax.io.JavaConverters._
    val str = this.getClass.getClassLoader.getResource("data/yoji.txt")
    val inp = str.asInput
    inp.lines()(Codec.UTF8).filter(_.length > 1).toSet
  }
}

class YojijukugoTagger extends UserScopedActor {
  def receive = {
    case PossibleTagRequest(wr, _) =>
      sender ! PossibleTags(if (Yojijukugo.data.contains(wr)) List("4j") else Nil)
  }
}
