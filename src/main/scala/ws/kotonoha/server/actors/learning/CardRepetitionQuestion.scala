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

package ws.kotonoha.server.actors.learning

import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.Full
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.examples.api.ExampleSentence
import ws.kotonoha.model.CardMode
import ws.kotonoha.server.actors.learning.RepeatBackend.RepQuestionPart
import ws.kotonoha.server.records.WordRecord

import scala.collection.immutable.BitSet.BitSet1

/**
  * @author eiennohito
  * @since 2016/08/29
  */
object CardRepetitionQuestion extends StrictLogging {
  def formatSimpleQuestion(w: WordRecord, mode: CardMode): (Int, Seq[RepQuestionPart]) = {
    mode match {
      case CardMode.Writing => -1 -> Seq(RepQuestionPart(w.writing.stris, None, false))
      case CardMode.Reading => -1 -> Seq(RepQuestionPart(w.reading.stris, None, false))
      case _ =>
        logger.warn(s"invalid card mode $mode")
        -1 -> Seq(RepQuestionPart(w.writing.stris, None, false))
    }
  }

  def formatSentenceQuestion(w: WordRecord, ex: ExampleSentence, mode: CardMode): Seq[RepQuestionPart] = {
    ex.units.map { u =>
      u.target match {
        case 0 => RepQuestionPart(u.content, u.reading, false)
        case _ =>
          mode match {
            case CardMode.Writing => RepQuestionPart(u.content, None, true)
            case CardMode.Reading => RepQuestionPart(u.reading.getOrElse(u.content), None, true)
            case _ => RepQuestionPart(u.content, None, true)
          }
      }
    }
  }

  def formatQuestion(w: WordRecord, mode: CardMode): (Int, Seq[RepQuestionPart]) = {
    val rexs = w.repExamples.valueBox
    rexs match {
      case Full(exs) =>
        val sents = exs.sentences
        if (sents.isEmpty) {
          return formatSimpleQuestion(w, mode)
        }

        val seen = new BitSet1(w.repExSeen.value)
        val maybeEx = mode match {
          case CardMode.Writing =>
            val kanji = UnicodeUtil.stream(w.writing.stris).filter(UnicodeUtil.isKanji).toSet
            sents.zipWithIndex.find { case (s, i) =>
              !seen.contains(i) &&
                s.units.filter(_.target != 0).exists(u => UnicodeUtil.stream(u.content).exists(cp => kanji.contains(cp)))
            }
          case CardMode.Reading =>
            sents.zipWithIndex.find { case (s, i) =>
              !seen.contains(i) &&
                s.units.exists { u =>
                  u.target != 0 &&
                    (UnicodeUtil.isKana(u.content) ||
                    u.reading.isDefined)
                }
            }
          case _ => None
        }

        maybeEx match {
          case Some((ex, i)) => (i, formatSentenceQuestion(w, ex, mode))
          case None => formatSimpleQuestion(w, mode)
        }
      case _ => formatSimpleQuestion(w, mode)
    }
  }
}
