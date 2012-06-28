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

package org.eiennohito.kotonoha.util

import org.eiennohito.kotonoha.records.AppConfig

/**
 * @author eiennohito
 * @since 20.06.12
 */

object StrokeType extends Enumeration {
  val Svgz, Svg, Png150, Png500 = Value
  type StrokeType = Value
}

object StrokesUtil {
  def strokeUri(cp: Int, st: StrokeType.StrokeType = StrokeType.Svgz): String = {
    st match {
      case StrokeType.Svg => "%s/uz/%04x.svg".format(AppConfig().stokeUri.is, cp)
      case StrokeType.Svgz => "%s/%04x.svgz".format(AppConfig().stokeUri.is, cp)
      case StrokeType.Png150 => "%s/p150/%04x.png".format(AppConfig().stokeUri.is, cp)
      case StrokeType.Png500 => "%s/p500/%04x.png".format(AppConfig().stokeUri.is, cp)
    }
  }

  def strokeUriStr(s: String, pos: Int, st: StrokeType.StrokeType = StrokeType.Svgz): String =
    strokeUri(s.codePointAt(pos), st)
}
