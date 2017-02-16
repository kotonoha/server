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

package ws.kotonoha.server.util

import java.util.Locale

import net.liftweb.http.S
import ws.kotonoha.server.lang.Iso6392
import ws.kotonoha.server.records.UserSettings

import scala.collection.mutable.ArrayBuffer

/**
 * @author eiennohito
 * @since 24.04.12
 */

object LangUtil {

  def okayLang(in: String): Boolean = {
    in match {
      case "eng" => true
      case _ =>
        jmdictLangs.contains(in)
    }
  }

  val langs = List("eng", "rus")

  val default = List("eng")

  val jmdictLangs = Set("eng", "ger", "rus", "hun", "ita", "spa", "dut", "fre", "swe", "slv")

  def acceptableFor(settings: UserSettings): Set[String] = langsFor(settings).toSet ++ default

  def langsFor(us: UserSettings): Seq[String] = {
    val stored = us.dictLangs.value
    if (stored.nonEmpty) return stored

    //else try to guess from request
    val req = S.getRequestHeader("Accept-Language")
    req.toOption match {
      case Some(s) =>
        val langs = parseAcceptLanguage(s)
        val codes = toLangCodes(langs)
        (default ++ codes).distinct
      case _ => default
    }
  }

  def toLangCodes(accepts: Seq[AcceptLanguage]): Seq[String] = {
    accepts.flatMap { al =>
      if (al.locale == null) { Nil }
      else {
        Iso6392.findByCode(al.locale.getISO3Language)
          .filter(l => okayLang(l.bibliographic) || okayLang(l.terminologic))
          .map(_.bibliographic)
      }
    }
  }

  private val codeRegex = "[qQ]=(\\d+(:?\\.\\d+))".r

  def parseAcceptLanguage(content: String): Seq[AcceptLanguage] = {
    //en-US,en;q=0.8,ja;q=0.6,ru;q=0.4
    val result = new ArrayBuffer[AcceptLanguage]()
    var currentWeight = 1.0f
    val parts = content.split(";")
    for (p <- parts) {
      val subparts = p.split(",")
      for (subp <- subparts) {
        val codeMatch = codeRegex.findFirstMatchIn(subp)
        codeMatch match {
          case Some(m) =>
            currentWeight = m.group(1).toFloat
          case _ =>
            val lang = subp
            val locale = Locale.forLanguageTag(lang)
            result += AcceptLanguage(
              weight = currentWeight,
              cultureCode = lang,
              locale = locale
            )
        }
      }
    }
    result
  }
}

case class AcceptLanguage(weight: Float, cultureCode: String, locale: Locale)
