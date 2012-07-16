package org.eiennohito.kotonoha.util

import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.dictionary.JMDictRecord
import org.eiennohito.kotonoha.japanese._
object WordUtils {
   def processWord(word:String): Option[String] = try {
     MongoDbInit.init()
     val word_type = JMDictRecord.query(word, None, 1).head.meaning.is.firstOption.flatMap(_.info.is.firstOption)
     ConjObj(word_type.getOrElse(""), word).masuForm
   } catch { case _ => None }
}
