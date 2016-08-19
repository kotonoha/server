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

package ws.kotonoha.server.examples.api

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import net.liftweb.common.{Empty, Failure, Full}
import net.liftweb.json.JsonAST.{JInt, JString, JValue}
import org.apache.commons.codec.binary.Hex
import ws.kotonoha.examples.api._
import ws.kotonoha.lift.json.{JFormat, JLCaseClass}

/**
  * @author eiennohito
  * @since 2016/08/15
  */
object ApiLift {

  implicit def pbufEnumJformat[E <: GeneratedEnum](implicit c: GeneratedEnumCompanion[E]) = new JFormat[E] {
    override def read(v: JValue) = v match {
      case JInt(i) => Full(c.fromValue(i.intValue()))
      case _ => Failure(s"input should be JInt, was $v")
    }

    override def write(o: E) = JInt(o.value)
  }

  implicit val bstringFmt = new JFormat[ByteString] {
    override def read(v: JValue) = v match {
      case JString(s) =>
        try {
          Full(ByteString.copyFrom(Hex.decodeHex(s.toCharArray)))
        } catch {
          case e: Exception => Failure(s"could not deconde string $s", Full(e), Empty)
        }
      case _ => Failure(s"input value should be JString, was $v")
    }

    override def write(o: ByteString) = {
      JString(new String(Hex.encodeHex(o.toByteArray)))
    }
  }

  implicit val unitFmt = JLCaseClass.format[SentenceUnit]
  implicit val sentFmt = JLCaseClass.format[ExampleSentence]
  implicit val packFmt = JLCaseClass.format[ExamplePack]

  implicit val queryFmt = JLCaseClass.format[ExampleQuery]
  implicit val preqFmt = JLCaseClass.format[ExamplePackRequest]

}



