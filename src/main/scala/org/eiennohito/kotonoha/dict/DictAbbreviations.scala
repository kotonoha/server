package org.eiennohito.kotonoha.dict

object DictAbbreviations {
  def Abbr2PartOfSpeechName(abbr:Option[String]):Option[String] =
    abbr match {
      case Some("n") => Some("名詞")
      case Some("v1") => Some("一段動詞")
      case Some("v5u") => Some("五段動詞・ア行")
      case Some("v5k") => Some("五段動詞・カ行")
      case Some("v5g") => Some("五段動詞・ガ行")
      case Some("v5s") => Some("五段動詞・シ行")
      case Some("v5t") => Some("五段動詞・ツ行")
      case Some("v5n") => Some("五段動詞・ナ行")
      case Some("v5b") => Some("五段動詞・バ行")
      case Some("v5m") => Some("五段動詞・マ行")
      case Some("v5r") => Some("五段動詞・ラ行")
      case Some("vk") => Some("動詞 （来る)")
      case Some(some) => Some(some)
      case _ => None
    }
}
