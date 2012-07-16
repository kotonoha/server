package org.eiennohito.kotonoha.util

import xml.Node
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.dictionary.JMDictRecord
import org.eiennohito.kotonoha.japanese._
import org.eiennohito.kotonoha.japanese.Conjuable._
object WordUtils {

   def processWord(word:String): Option[String] = try
     MongoDbInit.init()
     val q = JMDictRecord.query(word, None, 1)
     if (q.length != 1) None
     val word_type = q.head.meaning.is.firstOption.flatMap(_.info.is.firstOption)
     word_type match {
         //?
       //case Some("v1") => "【一段動詞】". ++ word.end("る", "ます").data
       //case Some("v5u") => "【五段動詞・ア行】" ++ word.end("う", "います").data
       case Some("v5k") => Some("【五段動詞・カ行】" ++ word.withLast(1)(_ => Some("きます")).data.get)
       case Some("v5g") => Some("【五段動詞・ガ行】" ++ word.withLast(1)(_ => Some("ぎます")).data.get)
       case Some("v5s") => Some("【五段動詞・シ行】" ++ word.withLast(1)(_ => Some("します")).data.get)
       case Some("v5t") => Some("【五段動詞・ツ行】" ++ word.withLast(1)(_ => Some("ちます")).data.get)
       case Some("v5n") => Some("【五段動詞・ナ行】" ++ word.withLast(1)(_ => Some("にます")).data.get)
       case Some("v5b") => Some("【五段動詞・バ行】" ++ word.withLast(1)(_ => Some("びます")).data.get)
       case Some("v5m") => Some("【五段動詞・マ行】" ++ word.withLast(1)(_ => Some("みます")).data.get)
       case Some("v5r") => Some("【五段動詞・ラ行】" ++ word.withLast(1)(_ => Some("ります")).data.get)
       case Some("vk") => Some("【来る】来ます")
       case _ if word.equals("する") => Some("【する】します")
       case Some("n") => Some("【名詞】")
       case Some(some: String) => Some("【種: " ++ some ++ "】")
       case _ => None
     }
   catch { case _ => None }
}
