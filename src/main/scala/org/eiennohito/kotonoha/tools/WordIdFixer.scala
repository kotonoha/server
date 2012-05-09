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

import scalax.io.Codec
import java.nio.charset.Charset
import org.joda.time.DateTime
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.{WordCardRecord, WordRecord}

/**
 * @author eiennohito
 * @since 09.05.12
 */

object WordIdFixer {
  import com.foursquare.rogue.Rogue._
  import org.eiennohito.kotonoha.util.KBsonDSL._
  def main(args: Array[String]) = {
    MongoDbInit.init()
    val obj = WordRecord where (_.createdOn before new DateTime(2012, 1, 1, 0, 0, 0)) orderDesc(_.createdOn) fetch(1)
    val after = WordRecord where (_.createdOn after new DateTime(2012, 1, 1, 0, 0, 0)) fetch()
    val o = obj.head
    val str = Stream.iterate(o.id.is)(_ + 1)
    val map = after.map(_.id.is).zip(str).toMap
    map map { case (k, v) =>
      WordCardRecord where (_.word eqs k) modify(_.word setTo v) updateMulti()
      val c = WordRecord.find(k)
      c map { _.delete_! }
      c map { _.id(v).save }
    }
  }
}
