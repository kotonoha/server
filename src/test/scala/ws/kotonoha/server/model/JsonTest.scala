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

package ws.kotonoha.server.model

import converters.DateTimeTypeConverter
import events.MarkEvent
import learning.{Container, WordCard, Word}
import net.liftweb.json._
import com.google.gson.GsonBuilder
import org.joda.time.DateTime
import ws.kotonoha.server.util.{OidSerializer, DateTimeUtils, ResponseUtil}
import ws.kotonoha.server.records.{WordCardRecord, ExampleRecord, WordRecord}
import java.io.InputStreamReader
import java.nio.charset.Charset
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.actors.schedulers.ReviewCard
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.server.actors.learning.WordsAndCards


/**
 * @author eiennohito
 * @since 31.01.12
 */

class JsonTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {

  val gson = {
    val gb = new GsonBuilder
    gb.registerTypeAdapter(classOf[DateTime], new DateTimeTypeConverter)
    gb.create()
  }

  val wid = ObjectId.createFromLegacyFormat(10, 11, 12)
  val cid = ObjectId.createFromLegacyFormat(10, 11, 13)

  def card = WordCardRecord.createRecord.word(wid).cardMode(CardMode.READING)

  def word = {
    val ex1 = ExampleRecord.createRecord.example("ex").translation("tr")
    val rec = WordRecord.createRecord
    rec.writing("wr").reading("re").meaning("me").examples(List(ex1)).id(wid)
    rec
  }

  test("word record becomes nice json") {
    val rec = WordRecord.createRecord
    rec.writing("hey" :: "pal" :: Nil).reading("guys")

    val js = rec.asJSON.toString()
    val rec2 = WordRecord.createRecord
    rec2.setFieldsFromJSON(js)
    rec.writing.is should equal(rec2.writing.is)
    rec.reading.is should equal(rec2.reading.is)
  }

  test("word record translates to java model") {
    val jv: JObject = word.asJValue
    val str = Printer.pretty(JsonAST.render(ResponseUtil.deuser(jv)))

    val obj = gson.fromJson(str, classOf[Word])
    obj.getMeaning should equal("me")
  }

  test("word card saves all right") {
    val jv = card.asJValue
    val str = Printer.compact(JsonAST.render(ResponseUtil.deuser(jv)))

    val obj = gson.fromJson(str, classOf[WordCard])
    obj.getCardMode should equal(CardMode.READING)
    obj.getWord should equal(wid.toString)
  }

  test("review card serializes") {
    import ws.kotonoha.server.model.learning.{ReviewCard => JReviewCard}
    implicit val formats = DefaultFormats ++ Seq(OidSerializer)
    val rc = new ReviewCard(new ObjectId(), "None", 321541232L)
    val json = Printer.compact(JsonAST.render(Extraction.decompose(rc)))

    val jrc = gson.fromJson(json, classOf[JReviewCard])
    jrc should have(
      'cid(rc.cid.toString),
      'seq(rc.seq),
      'source(rc.source)
    )
  }

  test("container is being parsed") {
    val rc = new ReviewCard(new ObjectId(), "None", 321541232L)
    val words = List(word, word)
    val cards = List(card, card)
    val rcards = List(rc, rc, rc)

    val jv = ResponseUtil.jsonResponse(WordsAndCards(words, cards, rcards))
    val str = Printer.compact(JsonAST.render(ResponseUtil.deuser(jv)))
    val obj = gson.fromJson(str, classOf[Container])
    obj.getWords.size() should be(2)
    obj.getCards.size() should be(2)
    obj.getSequence.size() should be(3)
    obj.getSequence
  }

  test("word mark event goes from java to scala world") {
    val event = new MarkEvent()
    event.setCard(cid.toString)
    event.setMark(5.0)
    event.setMode(CardMode.READING)
    event.setTime(1.0)
    val dt = DateTimeUtils.now
    event.setDatetime(dt)

    val str = gson.toJson(event)
    val jv = net.liftweb.json.parse(str)
    val rec = MarkEventRecord.createRecord
    rec.setFieldsFromJValue(jv)

    rec.card.is should equal(cid)
    rec.mark.is should equal(5.0)
    rec.mode.is should equal(CardMode.READING)
    rec.datetime.is should equal(dt)
  }

  import scala.collection.JavaConversions._

  test("loading json from file") {
    val str = getClass.getClassLoader.getResourceAsStream("json/scheduled.json")
    val src = new InputStreamReader(str, Charset.forName("UTF-8"))
    val obj = gson.fromJson(src, classOf[Container])
    val l2 = obj.getCards.map(_.getWord).toList.distinct.length
    val l3 = obj.getCards.map(_.getId).toList.distinct.length

    l2 shouldBe 45
    l3 shouldBe 46
  }
}
