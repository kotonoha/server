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

package org.eiennohito.kotonoha.records.dictionary

import com.weiglewilczek.slf4s.Logging

/**
 * @author eiennohito
 * @since 16.04.12
 */

object JMDictAnnotations extends Enumeration with Logging {

  class JMAnnotation(val short: String, val long: String) extends Val(nextId, short) {
  }

  protected def Value(short: String, lv: String) = new JMAnnotation(short, lv)

  val MA = Value("MA", "martial arts term")
  val X = Value("X", "rude or X-rated term (not displayed in educational software)")
  val abbr = Value("abbr", "abbreviation")
  val adji = Value("adj-i", "adjective (keiyoushi)")
  val adjna = Value("adj-na", "adjectival nouns or quasi-adjectives (keiyodoshi)")
  val adjno = Value("adj-no", "nouns which may take the genitive case particle `no'")
  val adjpn = Value("adj-pn", "pre-noun adjectival (rentaishi)")
  val adjt = Value("adj-t", "`taru' adjective")
  val adjf = Value("adj-f", "noun or verb acting prenominally")
  val adj = Value("adj", "former adjective classification (being removed)")
  val adv = Value("adv", "adverb (fukushi)")
  val advto = Value("adv-to", "adverb taking the `to' particle")
  val arch = Value("arch", "archaism")
  val ateji = Value("ateji", "ateji (phonetic) reading")
  val aux = Value("aux", "auxiliary")
  val auxv = Value("aux-v", "auxiliary verb")
  val auxadj = Value("aux-adj", "auxiliary adjective")
  val Buddh = Value("Buddh", "Buddhist term")
  val chem = Value("chem", "chemistry term")
  val chn = Value("chn", "children's language")
  val col = Value("col", "colloquialism")
  val comp = Value("comp", "computer terminology")
  val conj = Value("conj", "conjunction")
  val ctr = Value("ctr", "counter")
  val derog = Value("derog", "derogatory")
  val eK = Value("eK", "exclusively kanji")
  val ek = Value("ek", "exclusively kana")
  val exp = Value("exp", "Expressions (phrases, clauses, etc.)")
  val fam = Value("fam", "familiar language")
  val fem = Value("fem", "female term or language")
  val food = Value("food", "food term")
  val geom = Value("geom", "geometry term")
  val gikun = Value("gikun", "gikun (meaning as reading)  or jukujikun (special kanji reading)")
  val hon = Value("hon", "honorific or respectful (sonkeigo) language")
  val hum = Value("hum", "humble (kenjougo) language")
  val iK = Value("iK", "word containing irregular kanji usage")
  val id = Value("id", "idiomatic expression")
  val ik = Value("ik", "word containing irregular kana usage")
  val int = Value("int", "interjection (kandoushi)")
  val io = Value("io", "irregular okurigana usage")
  val iv = Value("iv", "irregular verb")
  val ling = Value("ling", "linguistics terminology")
  val msl = Value("m-sl", "manga slang")
  val male = Value("male", "male term or language")
  val malesl = Value("male-sl", "male slang")
  val math = Value("math", "mathematics")
  val mil = Value("mil", "military")
  val n = Value("n", "noun (common) (futsuumeishi)")
  val nadv = Value("n-adv", "adverbial noun (fukushitekimeishi)")
  val nsuf = Value("n-suf", "noun, used as a suffix")
  val npref = Value("n-pref", "noun, used as a prefix")
  val nt = Value("n-t", "noun (temporal) (jisoumeishi)")
  val num = Value("num", "numeric")
  val oK = Value("oK", "word containing out-dated kanji")
  val obs = Value("obs", "obsolete term")
  val obsc = Value("obsc", "obscure term")
  val ok = Value("ok", "out-dated or obsolete kana usage")
  val onmim = Value("on-mim", "onomatopoeic or mimetic word")
  val pn = Value("pn", "pronoun")
  val poet = Value("poet", "poetical term")
  val pol = Value("pol", "polite (teineigo) language")
  val pref = Value("pref", "prefix")
  val proverb = Value("proverb", "proverb")
  val prt = Value("prt", "particle")
  val physics = Value("physics", "physics terminology")
  val rare = Value("rare", "rare")
  val sens = Value("sens", "sensitive")
  val sl = Value("sl", "slang")
  val suf = Value("suf", "suffix")
  val uK = Value("uK", "word usually written using kanji alone")
  val uk = Value("uk", "word usually written using kana alone")
  val v1 = Value("v1", "Ichidan verb")
  val v2as = Value("v2a-s", "Nidan verb with 'u' ending (archaic)")
  val v4h = Value("v4h", "Yodan verb with `hu/fu' ending (archaic)")
  val v4r = Value("v4r", "Yodan verb with `ru' ending (archaic)")
  val v5 = Value("v5", "Godan verb (not completely classified)")
  val v5aru = Value("v5aru", "Godan verb - -aru special class")
  val v5b = Value("v5b", "Godan verb with `bu' ending")
  val v5g = Value("v5g", "Godan verb with `gu' ending")
  val v5k = Value("v5k", "Godan verb with `ku' ending")
  val v5ks = Value("v5k-s", "Godan verb - Iku/Yuku special class")
  val v5m = Value("v5m", "Godan verb with `mu' ending")
  val v5n = Value("v5n", "Godan verb with `nu' ending")
  val v5r = Value("v5r", "Godan verb with `ru' ending")
  val v5ri = Value("v5r-i", "Godan verb with `ru' ending (irregular verb)")
  val v5s = Value("v5s", "Godan verb with `su' ending")
  val v5t = Value("v5t", "Godan verb with `tsu' ending")
  val v5u = Value("v5u", "Godan verb with `u' ending")
  val v5us = Value("v5u-s", "Godan verb with `u' ending (special class)")
  val v5uru = Value("v5uru", "Godan verb - Uru old class verb (old form of Eru)")
  val vz = Value("vz", "Ichidan verb - zuru verb (alternative form of -jiru verbs)")
  val vi = Value("vi", "intransitive verb")
  val vk = Value("vk", "Kuru verb - special class")
  val vn = Value("vn", "irregular nu verb")
  val vr = Value("vr", "irregular ru verb, plain form ends with -ri")
  val vs = Value("vs", "noun or participle which takes the aux. verb suru")
  val vsc = Value("vs-c", "su verb - precursor to the modern suru")
  val vss = Value("vs-s", "suru verb - special class")
  val vsi = Value("vs-i", "suru verb - irregular")
  val kyb = Value("kyb", "Kyoto-ben")
  val osb = Value("osb", "Osaka-ben")
  val ksb = Value("ksb", "Kansai-ben")
  val ktb = Value("ktb", "Kantou-ben")
  val tsb = Value("tsb", "Tosa-ben")
  val thb = Value("thb", "Touhoku-ben")
  val tsug = Value("tsug", "Tsugaru-ben")
  val kyu = Value("kyu", "Kyuushuu-ben")
  val rkb = Value("rkb", "Ryuukyuu-ben")
  val nab = Value("nab", "Nagano-ben")
  val hob = Value("hob", "Hokkaido-ben")
  val vt = Value("vt", "transitive verb")
  val vulg = Value("vulg", "vulgar expression or word")
  val unknown = Value("unknown", "unknown")
  override type Value = JMAnnotation

  private lazy val namemap = {
    values.map(v => v.toString() -> v).toMap.withDefault{s => logger.warn("unknown value: " + s); unknown}
  }

  def safeValueOf(in: String) = {
    namemap(in)
  }
}
