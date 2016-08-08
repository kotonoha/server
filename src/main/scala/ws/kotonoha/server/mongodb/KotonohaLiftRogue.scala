/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.mongodb

import com.foursquare.field.{Field => RField}
import com.foursquare.rogue.{AbstractQueryField, DateTimeModifyField, DateTimeQueryField, LiftRogue}
import com.trueaccord.scalapb.GeneratedEnum
import net.liftweb.mongodb.record.BsonRecord
import ws.kotonoha.server.records.meta.{JodaDateField, PbEnumField}

import scala.language.implicitConversions

/**
 * @author eiennohito
 * @since 2013-07-24
 */
trait KotonohaLiftRogue extends LiftRogue {
  implicit def jodaDateTimeField2QueryField[M <: BsonRecord[M]](in: JodaDateField[M]): DateTimeQueryField[M] = {
    new DateTimeQueryField[M](in)
  }

  implicit def jodaDateTimeField2ModifyField[M <: BsonRecord[M]](in: JodaDateField[M]): DateTimeModifyField[M] = {
    new DateTimeModifyField[M](in)
  }

  implicit def pbEnumField2QueryField[M <: BsonRecord[M], E <: GeneratedEnum](in: PbEnumField[M, E]): PbEnumQueryField[M, E] = {
    new PbEnumQueryField[M, E](in)
  }
}

class PbEnumQueryField[M, E <: GeneratedEnum](fld: RField[E, M]) extends AbstractQueryField[E, E, Int, M](fld) {
  override def valueToDB(v: E) = v.value
}

object KotonohaLiftRogue extends KotonohaLiftRogue
