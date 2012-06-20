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

object StrokesUtil {
  def strokeUri(cp: Int): String = "%s/%04x.svgz".format(AppConfig().stokeUri.is, cp)
  def strokeUri(s: String, pos: Int): String = strokeUri(s.codePointAt(pos))
}
