package org.eiennohito.kotonoha.dict

object DictAbbreviations {
  def Abbr2PartOfSpeechName(abbr:Option[String]):Option[String] =
    abbr map {
      case "n" => "名詞"
      case "v1" => "一段動詞"
      case "v5u" => "五段動詞・ア行"
      case "v5k" => "五段動詞・カ行"
      case "v5g" => "五段動詞・ガ行"
      case "v5s" => "五段動詞・シ行"
      case "v5t" => "五段動詞・ツ行"
      case "v5n" => "五段動詞・ナ行"
      case "v5b" => "五段動詞・バ行"
      case "v5m" => "五段動詞・マ行"
      case "v5r" => "五段動詞・ラ行"
      case "vk" => "動詞 （来る)"
    }
}