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

package org.eiennohito.kotonoha.tools

import scalax.file.Path
import java.io.{InputStreamReader, FileInputStream}
import collection.immutable.PagedSeq
import util.parsing.input.StreamReader
import org.eiennohito.kotonoha.dict.WarodaiParser

/**
 * @author eiennohito
 * @since 05.04.12
 */

/**
 * Usage: first parameter is filename, second is arguments
 */
object WarodaiImporter extends App {
  val fn = args(0)
  val enc = args(1)

  val inp = new FileInputStream(fn)
  val reader = new InputStreamReader(inp, enc)
  val input = StreamReader(reader)
  val words = WarodaiParser.cards(input).get.toArray
  val i = 0
}
