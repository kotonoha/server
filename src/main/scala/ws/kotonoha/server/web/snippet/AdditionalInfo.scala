package ws.kotonoha.server.web.snippet

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Full
import ws.kotonoha.server.util.LangUtil
import com.weiglewilczek.slf4s.Logging
import ws.kotonoha.server.records.dictionary.{JMDictAnnotations, JMDictMeaning, JMString, JMDictRecord}

import ws.kotonoha.server.util.WordUtils.processWord

object AdditionalInfo {
  import net.liftweb.util.BindHelpers._
  def fld(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    bind("frm", in, AttrBindParam("value", q, "value"))
  }

  def response(in: NodeSeq): NodeSeq = {
    import net.liftweb.util.Helpers._
    val q = S.param("query").openOr("")
    bind("je", in, "response" -> processWord(q, None))
  }

}
