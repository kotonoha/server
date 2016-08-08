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

package ws.kotonoha.server.records.meta

import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import net.liftweb.common.{Empty, Full}
import net.liftweb.json.JValue
import net.liftweb.json.JsonAST.{JInt, JNothing}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.record.{Field, MandatoryTypedField, TypedField}
import ws.kotonoha.akane.utils.XInt

/**
  * @author eiennohito
  * @since 2016/08/08
  */
trait PbEnumTypedField[E <: GeneratedEnum] extends TypedField[E] {
  def companion: GeneratedEnumCompanion[E]

  override def setFromAny(in: Any) = in match {
    case i: Int => setFromInt(i)
    case XInt(i) => setFromInt(i)
    case _ => Empty
  }

  def setFromInt(i: Int): Full[E] = {
    val myval = companion.fromValue(i)
    set(myval.asInstanceOf[ValueType])
    Full(myval)
  }

  override def setFromString(s: String) = s match {
    case XInt(i) => setFromInt(i)
    case _ => Empty
  }

  override def asJValue = {
    val start = required_? match {
      case true => valueBox
      case _ => valueBox or defaultValueBox
    }
    start match {
      case Full(o) => JInt(o.value)
      case _ => JNothing
    }
  }

  override def setFromJValue(jvalue: JValue) = jvalue match {
    case JInt(n) => setFromInt(n.toInt)
    case _ => Empty
  }

  override def toForm = ???

  override def asJs = ???
}

class PbEnumField[OwnerType <: BsonRecord[OwnerType], E <: GeneratedEnum](val owner: OwnerType, dval: Int = 0)(implicit val companion: GeneratedEnumCompanion[E])
  extends Field[E, OwnerType] with MandatoryTypedField[E] with PbEnumTypedField[E] {

  def this(o: OwnerType, comp: GeneratedEnumCompanion[E]) = this(o)(comp)
  def this(o: OwnerType, comp: GeneratedEnumCompanion[E], dval: Int) = this(o, dval)(comp)
  def this(o: OwnerType, comp: GeneratedEnumCompanion[E], init: E) = this(o, init.value)(comp)
  override def defaultValue = companion.fromValue(dval)
}
