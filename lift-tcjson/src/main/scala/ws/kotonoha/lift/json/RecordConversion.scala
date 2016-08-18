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

package ws.kotonoha.lift.json

import net.liftweb.common.{Box, Full}
import net.liftweb.record.{MetaRecord, Record}

import scala.annotation.implicitNotFound

/**
  * @author eiennohito
  * @since 2016/08/18
  */
trait RecordConverter[T, Rec <: Record[Rec]] {
  def toRecord(o: T)(implicit meta: MetaRecord[Rec]): Rec = {
    val initial = meta.createRecord
    fillRecord(initial, o)
    initial
  }

  def fillRecord(r: Rec, o: T): Unit
  def fromRecord(o: Rec): Box[T]
}

@implicitNotFound("can't find ValueConverter ${L} <-> ${R}")
trait ValueConverter[L, R] { t =>
  def ltr(l: L): Box[R]
  def rtl(r: R): Box[L]
  def reverse: ValueConverter[R, L] = new ReversedValueConverter[R, L](this)
}

final class ReversedValueConverter[R, L](c: ValueConverter[L, R]) extends ValueConverter[R, L] {
  override def ltr(l: R) = c.rtl(l)
  override def rtl(r: L) = c.ltr(r)
  override def reverse: ValueConverter[L, R] = c
}

object ValueConverter {
  implicit object identity extends ValueConverter[Any, Any] {
    override def ltr(l: Any) = Full(l)
    override def rtl(r: Any) = Full(r)
  }

  //  implicit def reverse[L, R](implicit c: ValueConverter[L, R]): ValueConverter[R, L] = new ValueConverter[R, L] {
  //    override def ltr(l: R) = c.rtl(l)
  //    override def rtl(r: L) = c.ltr(r)
  //  }

  implicit def single[T]: ValueConverter[T, T] = identity.asInstanceOf[ValueConverter[T, T]]
}
