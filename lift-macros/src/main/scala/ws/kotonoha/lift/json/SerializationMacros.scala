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

import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.json.JsonAST.{JField, JValue}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  * @author eiennohito
  * @since 2016/08/12
  */

object JLCaseClass {
  def write[T <: Product]: JWrite[T] = macro SerializationMacros.writeCaseClass[T]
  def read[T <: Product]: JRead[T] = macro SerializationMacros.readCaseClass[T]
  def format[T <: Product]: JFormat[T] = macro SerializationMacros.formatCaseClass[T]
}

abstract class AbstractBuilder[T] {
  def appendField(fld: JField)
  def result(): Box[T]
  protected var errors: Box[Failure] = Empty
  def addError(f: Failure) = errors = Full(f.copy(chain = errors))
}

class SerializationMacros(val c: blackbox.Context) {
  import c.universe._

  def formatCaseClass[T](implicit  tt: WeakTypeTag[T]): Tree = {
    val q"new $rt { ..$wb }" = readCaseClass[T]
    val q"new $wt { ..$rb }" = writeCaseClass[T]

    val ft = weakTypeOf[JFormat[T]]

    q"new $rt with $wt with $ft { ..$wb ; ..$rb }"
  }

  def writeCaseClass[T](implicit tt: WeakTypeTag[T]): Tree = {
    val wr = weakTypeOf[JWrite[T]]
    val tp = weakTypeOf[T]
    val jv = weakTypeOf[JValue]

    val obj = TermName("obj")
    val bname = TermName("__bldr___")

    val decls = tp.decls.filter {
      case m: MethodSymbol => m.isCaseAccessor
      case _ => false
    }.map { i =>
      new FieldSpawner(i.name.decodedName.toString, i.info.resultType)
    }

    val bldr = reify { List.newBuilder[JField] }

    val tree = q"""
       new $wr {
         def write($obj: $tp): $jv = {
            val $bname = $bldr
            ..${decls.map(_.serializeField(obj, bname))}
            net.liftweb.json.JsonAST.JObject($bname.result())
         }
       }
     """
    //println(tree)
    tree
  }

  private val option = weakTypeOf[Option[Int]].typeConstructor

  class FieldSpawner(name: String, tp: Type) {
    val nm = TermName(name)
    val backingFieldName = TermName(s"___backing_field___$name")
    val backingVarName = TermName(s"___backing_var___$name")


    val isOptional = {
      val tpc = tp.typeConstructor
      tpc != NoType && tpc == option
    }

    val myType = if (isOptional) tp.typeArgs.head else tp

    val backingType = tq"net.liftweb.common.Box[$myType]"
    val tread = tq"ws.kotonoha.lift.json.JRead[$myType]"


    def backingField = {
      q"private[this] var $backingFieldName: $backingType = null"
    }

    def caseEmitter(tree: Tree) = {
      cq"$name => $backingFieldName = implicitly[$tread].read($tree)"
    }

    def backingVar = q"var $backingVarName: $tp = $varExpansion"

    val f =  TermName("f")

    def noValueError = q"""addError(net.liftweb.common.Failure("no value found for field " + $name)); ${defaultTrees(myType)}"""

    def varExpansion =
      q"""
       $backingFieldName match {
         case net.liftweb.common.Full($f) => ${if(isOptional) q"Some($f)" else q"f"}
         case null | net.liftweb.common.Empty =>
           ${if (isOptional) q"None" else noValueError}
         case e: net.liftweb.common.Failure =>
          addError(e)
          ${if (isOptional) q"None" else defaultTrees(myType)}
       }
      """

    def serializeField(obj: TermName, bldr: TermName): Tree = {
      val root = q"$obj.$nm"
      val access = if (isOptional) q"$root.get" else root
      val jout = tq"ws.kotonoha.lift.json.JWrite[$myType]"
      val jvtree = q"implicitly[$jout].write($access)"
      val rhs = q"new net.liftweb.json.JsonAST.JField($name, $jvtree)"
      if (isOptional) {
        q"""
           if ($root.isDefined) {
             $bldr += $rhs
           }
         """
      } else {
        q"$bldr += $rhs"
      }
    }

  }

  private val wttString = weakTypeOf[String]
  
  private val defaultTrees = Map[Type, Tree] (
    wttString -> q""" ""  """,
    WeakTypeTag.Int.tpe -> q"0",
    WeakTypeTag.Long.tpe -> q"0L",
    WeakTypeTag.Short.tpe -> q"0",
    WeakTypeTag.Byte.tpe -> q"0",
    WeakTypeTag.Boolean.tpe -> q"false",
    WeakTypeTag.Float.tpe -> q"0.0f",
    WeakTypeTag.Double.tpe -> q"0.0"
  ).withDefaultValue(q"null")

  def readCaseClass[T](implicit tt: WeakTypeTag[T]): Tree = {
    val readType = weakTypeOf[JRead[T]]
    val objType = weakTypeOf[Box[T]]
    val jvType = weakTypeOf[JValue]
    val jfType = weakTypeOf[JField]
    val bldrType = weakTypeOf[AbstractBuilder[T]]

    val input = TermName("jv")
    val bldr = TermName("bldr")

    val ourType = weakTypeOf[T]
    val decls = ourType.decls.filter {
      case m: MethodSymbol => m.isCaseAccessor
      case _ => false
    }.map {
      s => new FieldSpawner(s.name.encodedName.toString, s.info.resultType)
    }

    val resultBody =
      q"""
          ..${decls.map(_.backingVar)}
          if (this.errors.isEmpty) {
            net.liftweb.common.Full(
              new $ourType(..${decls.map(_.backingVarName)})
            )
          } else {
            this.errors.openOrThrowException("should be full here")
          }
      """

    val bldrTree = q"""
    new $bldrType {
       ..${decls.map(_.backingField)}
       def appendField(f: $jfType) = {
         f.name match {
           case ..${decls.map(_.caseEmitter(q"f.value"))}
           case _ =>
         }
       }
       def result(): $objType = $resultBody
    }
    """


    val tree = q"""
        new $readType {
          def read($input: $jvType): $objType = {
            $input match {
              case net.liftweb.json.JsonAST.JObject(fields) =>
                val bldr = $bldrTree
                val iter = fields.iterator
                while(iter.hasNext) {
                  val f: $jfType = iter.next()
                  f.value match {
                    case net.liftweb.json.JsonAST.JNull | net.liftweb.json.JsonAST.JNothing =>
                    case _ => bldr.appendField(f)
                  }
                }
                bldr.result()
              case _ => net.liftweb.common.Failure("input was not a JObject on top level " + $input)
            }
          }
        }

     """


    //println(tree)
    tree
  }
}

