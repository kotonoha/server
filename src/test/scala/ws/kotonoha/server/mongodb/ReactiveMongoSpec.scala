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

package ws.kotonoha.server.mongodb

import com.google.protobuf.ByteString
import com.mongodb.casbah.WriteConcern
import net.liftweb.common.Full
import net.liftweb.mongodb.record.field.ObjectIdPk
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.{OptionalIntField, StringField}
import net.liftweb.record.{Field, Record}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}
import ws.kotonoha.examples.api.{ExamplePack, ExampleSentence, PackStatus, SentenceUnit}
import ws.kotonoha.model.{CardMode, WordStatus}
import ws.kotonoha.server.records._
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaMongoRecord}
import ws.kotonoha.server.test.AkkaFree

import scala.concurrent.Await



class ReactiveRecord private() extends MongoRecord[ReactiveRecord] with ObjectIdPk[ReactiveRecord] {
  override def meta = ReactiveRecord

  object textfld extends StringField(this, 60)
  object opttest extends OptionalIntField(this)
  object opttest2 extends OptionalIntField(this)
  object date extends JodaDateField(this)
}

object ReactiveRecord extends ReactiveRecord with KotonohaMongoRecord[ReactiveRecord] with MongoMetaRecord[ReactiveRecord] with NamedDatabase


/**
  * @author eiennohito
  * @since 2016/08/10
  */
class ReactiveMongoSpec extends AkkaFree with Matchers with RecordMatchers {
  private val acc = kta.ioc.inst[RMData]

  import scala.concurrent.duration._

  def works[T <: MongoRecord[T] with ObjectIdPk[T]](init: T => T)(implicit meta: ReactiveMongoMeta[T]) = {
    val initial = meta.createRecord
    init(initial)
    val id = initial.id.get
    initial.save(WriteConcern.Safe)
    val loaded = Await.result(acc.byId[T](id), 1.second).get
    loaded should receq(initial)
    initial.delete_! shouldBe true
    Await.result(acc.save(Seq(initial)), 1.second).n shouldBe 1
    val found = meta.find(id)
    found.openOrThrowException("should be there") should receq(initial)
    Await.result(acc.delete(Seq(initial)), 1.second).n shouldBe 1
  }


  "Reactive Mongo" - {
    "works with a simple collection" in {
      works[ReactiveRecord] { r => r.textfld("dasfas").date(DateTime.now().withDayOfMonth(12)) }
    }

    "works with card" in {
      works[WordCardRecord] { r =>
        val l = ItemLearningDataRecord.createRecord
        l.lapse(1).repetition(2)
        r.user(new ObjectId()).enabled(true).cardMode(CardMode.Writing)
            .createdOn(DateTime.now).priority(100).tags(List("a", "b"))
            .learning(l)
      }
    }

    "works with word" in {
      works[WordRecord] { r =>
        val ex = ExampleRecord.createRecord.
          example("asdf").translation("dsfas")
        r.user(ObjectId.get()).tags(List("as", "das")).writing("sdas,dsa").examples(List(ex, ex)).status(WordStatus.Deleting)

        val su1 = SentenceUnit("test", reading = None, target = 0, morph = true)
        val su2 = SentenceUnit("me", reading = None, target = 1, morph = true)

        val s1 = ExampleSentence(
          id = ByteString.copyFromUtf8("jdlksajfasafsad"),
          Seq(su1, su2)
        )
        val x = ExamplePack(
          sentences = Seq(s1, s1),
          initialNum = 100,
          status = PackStatus.Ok,
          query = ""
        )

        r.repExamples(x).jmdictLink(123L)
      }
    }
  }
}


trait RecordMatchers {
  def receq(r: Record[_]) = new Matcher[Record[_]] {
    override def apply(l: Record[_]): MatchResult = {
      val lflds: Iterable[Field[_, _]] = l.allFields
      val iter = lflds.iterator
      while (iter.hasNext) {
        val lf = iter.next()
        r.fieldByName(lf.name) match {
          case Full(rf) =>
            if (!lf.valueBox.equals(rf.valueBox)) {
              return MatchResult(matches = false,
                s"for field ${lf.name} ${lf.valueBox} was not equal to ${rf.valueBox}",
                s"for field ${lf.name} ${lf.valueBox} was equal to ${rf.valueBox}")
            }
          case _ =>
            return MatchResult(matches = false, s"field ${lf.name} did not exist in $r", "TODO")
        }
      }
      MatchResult(matches = true, "objects were equal", "objects were not equal")
    }
  }
}
