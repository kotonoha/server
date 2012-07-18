package org.eiennohito.kotonoha.util

import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.dictionary.JMDictRecord
import org.eiennohito.kotonoha.japanese._
object WordUtils {
   def processWord(writing:String, reading:Option[String]): Option[String] = try {
     MongoDbInit.init()
     val word_type = JMDictRecord.query(writing, reading, 1).head.meaning.is.firstOption.flatMap(_.info.is.firstOption)
     ConjObj(word_type.getOrElse("exp"), writing).masuForm.data
   } catch { case _ => None }
}
