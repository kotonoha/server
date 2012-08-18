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

import xml.NodeSeq
import net.liftweb.http.S
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.util.{StrokeType, StrokesUtil, UnicodeUtil}

/**
 * @author eiennohito
 * @since 20.06.12
 */

object Kakijyun {
  import net.liftweb.util.Helpers._

  import UnicodeUtil._
  def sod150(s: String): NodeSeq = {
    val strokes = stream(s).
      filter{ k => isKanji(k) || isHiragana(k) || isKatakana(k) }.
      map {c => (Character.toChars(c), StrokesUtil.strokeUri(c, StrokeType.Png150))}
    strokes.flatMap { case (c, uri) =>
        <img src={uri} alt={new String(c)}></img> }
  }

  def sod500(s: String): NodeSeq = {
    val strokes = stream(s).
      filter{ k => isKanji(k) || isHiragana(k) || isKatakana(k) }.
      map {c => (Character.toChars(c), StrokesUtil.strokeUri(c, StrokeType.Png500))}
    strokes.flatMap { case (c, uri) =>
        <img src={uri} alt={new String(c)}></img> }
  }

  def japSod(s: String, tp: StrokeType.StrokeType = StrokeType.Png150): NodeSeq = {
    val strokes = stream(s).
      filter{ k => isKanji(k) || isHiragana(k) || isKatakana(k) }.
      map {c => (Character.toChars(c), StrokesUtil.strokeUri(c, tp))}
    strokes.flatMap { case (c, uri) =>
        <img src={uri} alt={new String(c)}></img> }
  }

  def kanjiSod(s: String, tp: StrokeType.StrokeType = StrokeType.Png150): NodeSeq = {
    val strokes = stream(s).
      filter{ k => isKanji(k) }.
      map {c => (Character.toChars(c), StrokesUtil.strokeUri(c, tp))}
    strokes.flatMap { case (c, uri) =>
        <img src={uri} alt={new String(c)}></img> }
  }

  def sods(s: String): NodeSeq = {
    val strokes = stream(s).
      filter(UnicodeUtil.isKanji(_)).map(StrokesUtil.strokeUri(_))
    strokes.flatMap { uri => <object data={uri} type={"image/svg+xml"} /> }
  }

  def render(in: NodeSeq): NodeSeq = {
    val q = S.param("q").getOrElse("書き順")
    val tf = "#sentence-input [value]" #> q &
              "#content *" #> sods(q)
    tf(in)
  }
}
