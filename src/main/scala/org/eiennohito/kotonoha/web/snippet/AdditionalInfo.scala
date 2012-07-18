package org.eiennohito.kotonoha.web.snippet

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Full
import org.eiennohito.kotonoha.util.LangUtil
import com.weiglewilczek.slf4s.Logging
import org.eiennohito.kotonoha.records.dictionary.{JMDictAnnotations, JMDictMeaning, JMString, JMDictRecord}

import org.eiennohito.kotonoha.util.WordUtils.processWord

object AdditionalInfo {
  import net.liftweb.util.BindHelpers._
  def fld(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    bind("frm", in, AttrBindParam("value", q, "value"))
  }

  def response(in: NodeSeq): NodeSeq = {
    import net.liftweb.util.Helpers._
    val q = S.param("query").openOr("")
    bind("je", in, "response" -> processWord(q, None).getOrElse(""))
  }

}
