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

package org.eiennohito.kotonoha.actors.model




import akka.util.Timeout
import akka.actor.{ActorLogging, Actor}
import org.eiennohito.kotonoha.model.CardMode
import org.eiennohito.kotonoha.actors.{KotonohaMessage, SaveRecord, RootActor}
import org.eiennohito.kotonoha.records.{WordStatus, WordRecord}
import akka.dispatch.{ExecutionContext, Future}

trait WordMessage extends KotonohaMessage
case class RegisterWord(word: WordRecord) extends WordMessage
case class ChangeWordStatus(word: Long, status: WordStatus.Value)

class WordActor extends Actor with ActorLogging with RootActor {
  import akka.util.duration._
  import akka.pattern.ask
  import com.foursquare.rogue.Rogue._

  implicit val timeout: Timeout = 1 second
  implicit val dispatcher = context.dispatcher

  def checkReadingWriting(word: WordRecord) = {
    val read = word.reading.is
    val writ = word.writing.is
    !read.equalsIgnoreCase(writ)
  }

  protected def receive = {
    case RegisterWord(word) => {
      val userId = word.user.is
      val wordid = word.id.is
      var fl = List(ask(root, SaveRecord(word)))
      if (checkReadingWriting(word)) {
        fl ::= root ? RegisterCard(wordid, userId, CardMode.READING)
      }
      fl ::= root ? RegisterCard(wordid, userId, CardMode.WRITING)

      val toReply = sender
      val s = Future.sequence(fl.map(_.mapTo[Boolean]))
      s foreach {
        list =>
          toReply ! word.id.is
      }
    }
    case ChangeWordStatus(word, stat) => {
      val q = WordRecord where (_.id eqs word) modify(_.status setTo stat)
      q.updateOne()
      sender ! 1
    }
  }
}
