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

package org.eiennohito.kotonoha.dict

import util.parsing.input.CharSequenceReader

class WarodaiParserTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {

  import WarodaiParser._

  implicit def str2charseqreader(in: String) = new CharSequenceReader(in)

  test("parses reading successfully") {
    val text = "あかちゃ, あかちゃいろ"
    val rr = reading(text)
    rr.successful should be (true)
    rr.get should have size (2)
    rr.get.head should equal ("あかちゃ")
    rr.get.tail.head should equal ("あかちゃいろ")
  }

  test("parses writing successfully") {
    val text = "【赤茶, 赤茶色】"
    val wr = writing(text)
    wr.successful should be (true)
    wr.get should have size (2)
    wr.get.head should equal ("赤茶")
  }

  test("parses russian reading correctly") {
    val text = "(абаё)"
    val rr = rusReading(text)
    rr.get should have length (1)
    rr.get.head should equal ("абаё")
  }

  test("parses identifier correctly") {
    val text = "〔1;1;8〕"
    val pr = identifier(text)
    pr.successful should be (true)
    pr.get.num should equal (8)
  }

  test("parses what identifier correctly") {
    val text = "〔1;0185(-0184)〕"
    val pr = identifier(text)
    pr.successful should be (true)
    pr.get.num should equal (184)
  }

  test("parses what2 identifier correctly") {
      val text = "〔1;0368(-0367,+0369)〕"
      val pr = identifier(text)
      pr.successful should be (true)
      pr.get.num should equal (367)
    }

  test("parses whole header") {
    val text = "あばよ(абаё)〔1;1;8〕"
    val h = header(text)
    h.successful should be (true)
    val hdr = h.get
    hdr.rusReadings.head should equal ("абаё")
  }

  test("parses simple word entry") {
    val text = """あばよ(абаё)〔1;1;8〕
    пока!, всего!"""
    val crd = card(text)
    crd.successful should be (true)
  }

  test("parses 2 entries") {
    val text = """あわせもの【合わせ物】(авасэмоно)〔1;3;28〕
    <i>связ.:</i> 合わせ物は離れ物 <i>погов.</i> что соединяется, то и разъединяется.
    • Также 【合せ物】.

    あわせめ【合わせ目】(авасэмэ)〔1;3;29〕
    шов; соединение; стык;
    合わせ目から裂ける рваться по шву.
    • Также 【合せ目】.
    """.trim
    val crds = cards(text)
    crds.successful should be (true)
    crds.get should have length (2)
  }

  test("parses 3 words") {
    val text = """
    あがく【足掻く】(агаку)〔1;3;47〕
    бить копытом о землю <i>(о лошади)</i>; <i>обр.</i> делать отчаянные усилия

    あがめる【崇める】 (агамэру)〔1;3;48〕
    почитать; поклоняться;
    神と崇める обожествлять;
    …を師と崇める почитать <i>кого-л.</i> как своего учителя.

    あがない【贖い】 (аганаи)〔1;3;49〕
    компенсация, возмещение;
    罪の贖いをする искупать вину.""".trim

    val crds = cards(text)
    crds.successful should be (true)
    crds.get should have length (3)
  }

  test("parse strange") {
    val text = """ウェスタン,ウェスターン(ўэсўтан, ўэсўта:н)〔2;373;47〕
    (<i>англ.</i> Western) западный.
    • В БЯРС пропущен вариант русской транскрипции с коротким слогом "та": ўэсўта:н ウェスタ[ー]ン.
    """.trim

    val crd = card(text)
    crd.successful should be (true)
  }

  test("parse lineends") {
    "\n\r\n\n\r\r\n"
  }

}
