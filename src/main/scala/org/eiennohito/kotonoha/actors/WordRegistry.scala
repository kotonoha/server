package org.eiennohito.kotonoha.actors

import org.eiennohito.kotonoha.model.CardMode
import org.eiennohito.kotonoha.records.{WordCardRecord, WordRecord}
import akka.dispatch.Future
import net.liftweb.common.Empty
import akka.actor.{ActorLogging, Actor}
import akka.util.Timeout
import org.eiennohito.kotonoha.util.DateTimeUtils

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
/**
 * @author eiennohito
 * @since 02.02.12
 */

case class RegisterWord(word: WordRecord)
case class RegisterCard(word: Long, userId: Long, cardMode: Int)

class WordRegistry extends Actor with RootActor with ActorLogging {
  import akka.pattern.{ask, pipe}
  import akka.util.duration._

  implicit val timeout: Timeout = 1 second
  implicit def system = context.system

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
        fl ::= self ? RegisterCard(wordid, userId, CardMode.READING)
      }
      fl ::= self ? RegisterCard(wordid, userId, CardMode.WRITING)

      val toReply = sender
      val s = Future.sequence(fl.map(_.mapTo[Boolean]))
      s foreach { list =>
        toReply ! word.id.is
      }
    }

    case RegisterCard(word, user, mode) => {
      import DateTimeUtils._
      val card = WordCardRecord.createRecord
      card.user(user).word(word).cardMode(mode).learning(Empty).notBefore(now)
      val s = sender
      ask(root, SaveRecord(card)) pipeTo (s)
    }
  }
}
