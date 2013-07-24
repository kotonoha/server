package ws.kotonoha.server.wiki.template

import scala.xml.NodeSeq
import ws.kotonoha.server.wiki.WikiRenderer

/**
 * @author eiennohito
 * @since 2013-07-24
 */
object ExampleTemplate extends Template {
  def apply(in: String): NodeSeq = {
    <span class="nihongo example">{WikiRenderer.parseMarkdown(in, url)}</span>
  }
}

object JapaneseTemplate extends Template {
  def apply(in: String) = <span>{in}</span>
}
