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

import net.liftweb.common.{Box, Empty, Failure}
import net.liftweb.record.{Field, MetaRecord, Record}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  * @author eiennohito
  * @since 2016/08/15
  */
trait RecordConverter[T, Rec <: Record[Rec]] {
  def toRecord(o: T)(implicit meta: MetaRecord[Rec]): Rec
  def fromRecord(o: Rec): Box[T]
}


object JLRecord {
  def cnv[O, T <: Record[T]]: RecordConverter[O, T] = macro RecordSerializationMacros.recordConverter[O, T]
}

class RecordSerializationMacros(val c: blackbox.Context)  {
  import c.universe._

  private val option = weakTypeOf[Option[Int]].typeConstructor

  private val defaultTrees = Map[Type, Tree] (
    WeakTypeTag.Int.tpe -> q"0",
    WeakTypeTag.Long.tpe -> q"0L",
    WeakTypeTag.Short.tpe -> q"0",
    WeakTypeTag.Byte.tpe -> q"0",
    WeakTypeTag.Boolean.tpe -> q"false",
    WeakTypeTag.Float.tpe -> q"0.0f",
    WeakTypeTag.Double.tpe -> q"0.0"
  ).withDefaultValue(q"null")

  case class DataClassFieldInfo(tpe: Type, name: TermName) {
    def fillRec(param: TermName, recName: TermName, rec: Option[RecordFieldInfo]): Tree = {
      rec match {
        case None => EmptyTree
        case Some(r) =>
          val access = q"$param.$name"
          isOptional match {
            case true =>
              q"""
                  if ($access.isDefined) {
                    $recName.${r.name}($access.get)
                  }
               """
            case false => q"$recName.${r.name}($access)"
          }
      }
    }

    def varDef(paramName: TermName, rec: Option[RecordFieldInfo], fail: TermName): Tree = {
      rec match {
        case None => EmptyTree
        case Some(r) =>
          val bdy = isOptional match {
            case true =>
              q"""$paramName.${r.name}.valueBox match {
                    case f: net.liftweb.common.Full[_] => Some(f.value)
                    case net.liftweb.common.Empty => None
                    case f: net.liftweb.common.Failure =>
                      $fail = net.liftweb.common.Full(f.copy(chain = $fail))
                      None
                 }
               """
            case _ =>
              q"""$paramName.${r.name}.valueBox match {
                    case f: net.liftweb.common.Full[_] => f.value
                    case net.liftweb.common.Empty =>
                      $fail = net.liftweb.common.Full(net.liftweb.common.Failure(${s"$name was empty"}, net.liftweb.common.Empty, $fail))
                      ${defaultTrees(tpe)}
                    case f: net.liftweb.common.Failure =>
                      $fail = net.liftweb.common.Full(f.copy(chain = $fail))
                      ${defaultTrees(tpe)}
                 }
               """
          }
          q"var $varName: $tpe = $bdy"
      }
    }

    val isOptional = {
      val tpc = tpe.typeConstructor
      tpc != NoType && tpc == option
    }

    val varName = TermName(s"__var__$name")
  }

  val liftField = weakTypeOf[Field[_, _]]

  case class RecordFieldInfo(fldTpe: Type, name: TermName) {
    val tpe = fldTpe.typeArgs.head
  }

  def recordConverter[O, T <: Record[T]](implicit ot: WeakTypeTag[O], rt: WeakTypeTag[T]): Tree = {
    val wr = weakTypeOf[RecordConverter[O, T]]
    val boxOt = weakTypeOf[Box[O]]

    val recflds = rt.tpe.members.collect {
      case ms: MethodSymbol if ms.paramLists.isEmpty && ms.returnType <:< liftField =>
        val tpe = ms.returnType.resultType.baseType(liftField.typeSymbol)
        RecordFieldInfo(tpe, ms.name)
      case ms: ModuleSymbol if ms.typeSignature <:< liftField =>
        val tpe = ms.typeSignature.baseType(liftField.typeSymbol)
        RecordFieldInfo(tpe, ms.name)
    }.toList

    val cases = ot.tpe.decls.filter {
      case m: MethodSymbol => m.isCaseAccessor
      case _ => false
    }.map {
      case s: MethodSymbol => DataClassFieldInfo(s.info.resultType, s.name)
    }.toList

    val paramName = TermName("_input__")
    val failName = TermName("fail__boxed_")

    val fromBody =
      q"""
       var $failName: ${typeOf[Box[Failure]]} = ${reify(Empty)}
       ..${cases.map(c => c.varDef(paramName, recflds.find(_.name == c.name), failName))}
       $failName match {
         case net.liftweb.common.Full(f) => f
         case _ =>
          val x = new ${ot.tpe.typeSymbol}(..${cases.map(_.varName)})
          net.liftweb.common.Full(x)
       }
       """

    val recName = TermName("rec")
    val toBody =
      q"""{
          val $recName = meta.createRecord
          ..${cases.map(c => c.fillRec(paramName, recName, recflds.find(_.name == c.name)))}
          $recName
         }
       """

    val metaTp = weakTypeOf[MetaRecord[T]]

    val tree = q"""
     new $wr {
       def fromRecord($paramName: $rt): $boxOt = $fromBody
       def toRecord($paramName: $ot)(implicit meta: $metaTp): $rt = $toBody
     }
     """

    //println(tree)
    tree
  }
}
