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
import net.liftweb.json.JsonAST._

import scala.annotation.implicitNotFound
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/08/12
  */


@implicitNotFound("could not find JWrite for type ${T}")
trait JWrite[T] {
  def write(o: T): JValue
}

object JWrite {
  implicit object intWrite extends JWrite[Int] {
    override def write(o: Int) = JInt(o)
  }

  implicit object longWrite extends JWrite[Long] {
    override def write(o: Long) = JInt(o)
  }

  implicit object stringWrite extends JWrite[String] {
    override def write(o: String) = JString(o)
  }

  implicit object boolWrite extends JWrite[Boolean] {
    override def write(o: Boolean) = JBool(o)
  }

  implicit object floatWrite extends JWrite[Float] {
    override def write(o: Float) = JDouble(o)
  }

  implicit object doubleWrite extends JWrite[Double] {
    override def write(o: Double) = JDouble(o)
  }

  implicit def optWrite[T](implicit w: JWrite[T]): JWrite[Option[T]] = new JWrite[Option[T]] {
    override def write(o: Option[T]) = {
      if (o.isDefined) w.write(o.get)
      else JNothing
    }
  }

  implicit def arrayWrite[T](implicit w: JWrite[T]): JWrite[Array[T]] = new JWrite[Array[T]] {
    override def write(o: Array[T]) = {
      val objs = o.view.map(w.write).toList
      JArray(objs)
    }
  }

  implicit def seqWrite[Tp[_], T](implicit w: JWrite[T], ev: Tp[T] <:< Traversable[T]): JWrite[Tp[T]] = new JWrite[Tp[T]] {
    override def write(o: Tp[T]) = {
      val objs = o.view.map(w.write).toList
      JArray(objs)
    }
  }
}

trait JRead[T] {
  def read(v: JValue): Box[T]
}

object JRead {
  @inline private def box[T](f: => T): Box[T] = {
    try {
      Full(f)
    } catch {
      case e: Exception => Failure("error when reading", Full(e), Empty)
    }
  }

  implicit object intRead extends JRead[Int] {
    override def read(v: JValue) = v match {
      case JInt(i) => box(i.intValue())
      case _ => Failure(s"invalid jvalue $v, should be JInt")
    }
  }

  implicit object longRead extends JRead[Long] {
    override def read(v: JValue) = v match {
      case JInt(i) => box(i.longValue())
      case _ => Failure(s"invalid jvalue $v, should be JInt")
    }
  }

  implicit object stringRead extends JRead[String] {
    override def read(v: JValue) = v match {
      case JString(s) => Full(s)
      case _ => Failure(s"invalid jvalue, should be JString")
    }
  }

  implicit object boolRead extends JRead[Boolean] {
    override def read(v: JValue) = v match {
      case JBool(i) => Full(i)
      case _ => Failure(s"invalid jvalue $v, should be JBool")
    }
  }

  implicit object floatRead extends JRead[Float] {
    override def read(v: JValue) = v match {
      case JDouble(d) => box(d.toFloat)
      case JInt(i) => box(i.floatValue())
      case _ => Failure(s"invalid jvalue $v, should be JDouble")
    }
  }

  implicit object doubleRead extends JRead[Double] {
    override def read(v: JValue) = v match {
      case JDouble(d) => Full(d)
      case JInt(i) => Full(i.doubleValue())
      case _ => Failure(s"invalid jvalue $v, should be JDouble")
    }
  }

  implicit def optWrite[T](implicit r: JRead[T]): JRead[Option[T]] = new JRead[Option[T]] {
    override def read(v: JValue) = v match {
      case JNothing => Full(None)
      case jv => r.read(jv).map(x => Some(x))
    }
  }

  implicit def arrRead[T](implicit r: JRead[T], ct: ClassTag[T]): JRead[Array[T]] = {
    new JRead[Array[T]] {
      private val seqread = implicitly[JRead[Seq[T]]]
      override def read(v: JValue) = seqread.read(v).map(_.toArray)
    }
  }

  implicit def seqRead[SC[_], T](implicit r: JRead[T], cbf: CanBuildFrom[_, T, SC[T]]): JRead[SC[T]] = new JRead[SC[T]] {
    private[this] def parseArray(vs: List[JValue], builder: mutable.Builder[T, SC[T]]) = {
      var failed: Box[Failure] = Empty
      val iter = vs.iterator
      while (iter.hasNext) {
        val o = iter.next()
        r.read(o) match {
          case f: Full[T] => builder += f.value
          case f: Failure => failed = Full(f.copy(chain = failed))
          case _ => //ignore
        }
      }
      failed match {
        case f: Full[Failure] => f.value
        case _ => Full(builder.result())
      }
    }

    override def read(v: JValue) = {
      v match {
        case JArray(vs) => parseArray(vs, cbf())
        case _ => Failure(s"top level element was not JArray, but $v")
      }
    }
  }
}

trait JFormat[T] extends JRead[T] with JWrite[T]

final class WrappedFormat[T](r: JRead[T], w: JWrite[T]) extends JFormat[T] {
  override def read(v: JValue) = r.read(v)
  override def write(o: T) = w.write(o)
}

object JFormat {
  implicit def readAndWriteToFormat[T](implicit r: JRead[T], w: JWrite[T]): JFormat[T] = new WrappedFormat[T](r, w)
}
