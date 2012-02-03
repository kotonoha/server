package org.eiennohito.kotonoha.actors

import akka.actor.Actor
import org.eiennohito.kotonoha.model.CardMode
import org.eiennohito.kotonoha.records.{WordCardRecord, WordRecord}
import akka.dispatch.Future
import net.liftweb.common.Empty
import akka.event.LoggingReceive

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
case class RegisterCard(wordId: Future[Long], userId: Long, cardMode: Int)

class WordRegistry extends Actor {
  implicit def system = context.system
  protected def receive = LoggingReceive(this) {
    case RegisterWord(word) => {
      val userId = word.user.is
      val f = Future {
        word.save
        word.id.is
      } (context.dispatcher)
      self ! RegisterCard(f, userId, CardMode.READING)
      self ! RegisterCard(f, userId, CardMode.WRITING)
      val toReply = sender
      f foreach (
        toReply ! _
      )
    }
    case RegisterCard(wordF, user, mode) => {
      val card = WordCardRecord.createRecord
      card.user(user).cardMode(mode).learning(Empty)
      wordF.onSuccess { case w => card.word(w).save }
    }
  }
}
