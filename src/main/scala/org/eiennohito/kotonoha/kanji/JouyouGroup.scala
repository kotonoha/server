package org.eiennohito.kotonoha.kanji

import collection.immutable.HashMap

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
 * @since 02.03.12
 */

object KanjiType extends Enumeration {
  type KanjiType = Value
  val Old, New, Removed, Absent = Value
}

object Jouyou {

  lazy private val lookupTable = {
    val kanji = JouyouKanji.kanji
    val from = JouyouKanji.newFrom
    val old = kanji.toStream.take(from).zipWithIndex.
      filterNot {  case (k, i) => JouyouKanji.removed.contains(i) }.
      map { case (k, i) => k -> KanjiType.Old }
    val newK = kanji.drop(from).toStream.map(_ -> KanjiType.New)
    val rem = JouyouKanji.removed.map{kanji(_)}.map(_ -> KanjiType.Removed)
    new HashMap[String, KanjiType.Value] ++ old ++ newK ++ rem
  }

  def category(k: String) = {
    val cat = lookupTable.get(k)
    cat.getOrElse(KanjiType.Absent)
  }

}
