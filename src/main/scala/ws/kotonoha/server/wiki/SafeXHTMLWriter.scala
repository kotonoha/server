/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.wiki

/**
 * @author eiennohito
 * @since 20.04.13 
 */

import scala.util.Random
import scala.xml.{Text => XMLText, _}
import scala.xml.Group
import com.tristanhunt.knockoff._

class SafeXHTMLWriter(urls: String => Option[WikiUrl]) {

  /** Backwards compatibility? *cough* */
  def toXML( blocks : Seq[Block] ) : NodeSeq = toXHTML( blocks )

  /** Creates a Group representation of the document. */
  def toXHTML( blocks : Seq[Block] ) : NodeSeq = blocks.flatMap( blockToXHTML(_) )

  def haveId(data: MetaData, map: Map[String, Seq[Span]]) = {
    data.find(e => e.key == "id").map(_.value).flatMap {
      case Seq(XMLText(c)) => map.get(c)
      case _ => None
    }
  }

  def joinMd(seq: NodeSeq, md: Map[String, Seq[Span]]): NodeSeq = {
    val ns = seq.flatMap {
      case e @ Elem(null, "span", data, _, _) =>
        val trial = haveId(data, md)
        if (trial.isEmpty) e
        else {
          val nodes = trial.get
          nodes.flatMap(spanToXHTML)
        }
      case x => x
    }
    ns
  }

  def blockToXHTML(block: Block) : NodeSeq = block match {
    case Paragraph( spans, _ ) => paragraphToXHTML( spans )
    case Header( level, spans, _ ) => headerToXHTML( level, spans )
    case LinkDefinition( _, _, _, _ ) => NodeSeq.Empty
    case Blockquote( children, _ ) => blockquoteToXHTML( children )
    case CodeBlock( text, _ ) => codeToXHTML( text )
    case HorizontalRule( _ ) => hrXHTML
    case OrderedItem( children, _ ) => liToXHTML( children )
    case UnorderedItem( children, _ ) => liToXHTML( children )
    case OrderedList( items ) => olToXHTML( items )
    case UnorderedList( items ) => ulToXHTML( items )
    case HTMLBlock( content, _ ) => NodeSeq.Empty
    case SanitizedHtmlBlock(nodes, md, _) => joinMd(nodes, md)
  }

  def htmlBlockToXHTML(html: String): NodeSeq = Unparsed( html )

  def paragraphToXHTML : Seq[Span] => NodeSeq = spans => {
    def isHTML( s : Span ) = s match {
      case y : HTMLSpan => true
      case Text( content ) => if ( content.trim.isEmpty ) true else false
      case _ => false
    }
    if ( spans.forall( isHTML ) )
      spans.flatMap( spanToXHTML(_) )
    else
      <p>{ spans.flatMap( spanToXHTML(_) ) }</p>
  }

  def headerToXHTML : ( Int, Seq[Span] ) => NodeSeq = (level, spans) => {
    val spanned = spans.flatMap( spanToXHTML(_) )
    level match {
      case 1 => <h1>{ spanned }</h1>
      case 2 => <h2>{ spanned }</h2>
      case 3 => <h3>{ spanned }</h3>
      case 4 => <h4>{ spanned }</h4>
      case 5 => <h5>{ spanned }</h5>
      case 6 => <h6>{ spanned }</h6>
      case _ => <div class={ "header" + level }>{ spanned }</div>
    }
  }

  def templater(content: String): NodeSeq = WikiTemplates(content)

  def blockquoteToXHTML : Seq[Block] => NodeSeq =
    children => <blockquote>{ children.map( blockToXHTML(_) ) }</blockquote>

  def codeToXHTML : Text => NodeSeq =
    text => <pre><code>{ text.content }</code></pre>

  def hrXHTML : NodeSeq = <hr/>

  def liToXHTML : Seq[Block] => NodeSeq =
    children => <li>{ simpleOrComplex( children ) }</li>

  private def simpleOrComplex( children : Seq[Block] ): NodeSeq = {
    if ( children.length == 1 )
      children.head match {
        case Paragraph( spans, _ ) => spans.flatMap( spanToXHTML(_) )
        case _ => children.flatMap( blockToXHTML(_) )
      }
    else
      children.flatMap( blockToXHTML(_) )
  }

  def olToXHTML : Seq[Block] => NodeSeq =
    items => <ol>{ items.map( blockToXHTML(_) ) }</ol>

  def ulToXHTML : Seq[Block] => NodeSeq =
    items => <ul>{ items.map( blockToXHTML(_) ) }</ul>

  def spanToXHTML(span: Span): NodeSeq = span match {
    case Text( content ) => textToXHTML( content )
    case HTMLSpan( html ) => Nil
    case SanitizedHtmlSpan(ns, md) => joinMd(ns, md)
    case CodeSpan( code ) => codeSpanToXHTML( code )
    case Strong( children ) => strongToXHTML( children )
    case Emphasis( children ) => emphasisToXHTML( children )
    case Link( children, url, title ) => linkToXHTML( children, url, title )
    case IndirectLink( children, definition ) =>
      linkToXHTML( children, definition.url, definition.title )
    case ImageLink( children, url, title ) => imageLinkToXHTML( children, url, title )
    case IndirectImageLink( children, definition ) =>
      imageLinkToXHTML( children, definition.url, definition.title )
    case Template(data) => templater(data)
  }

  def textToXHTML : String => NodeSeq = content => XMLText( Unescape.unescape(content) )

  def htmlSpanToXHTML(html: String):  NodeSeq = Unparsed( html )

  def codeSpanToXHTML : String => NodeSeq = code => <code>{ code }</code>

  def strongToXHTML : Seq[Span] => NodeSeq =
    spans => <strong>{ spans.map( spanToXHTML(_) ) }</strong>

  def emphasisToXHTML : Seq[Span] => NodeSeq =
    spans => <em>{ spans.map( spanToXHTML(_) ) }</em>

  def linkToXHTML(spans: Seq[Span], url: String, title: Option[String]): NodeSeq = {
    urls(url) match {
      case None => NodeSeq.Empty
      case Some(x) =>
        val base = <a href={x.url} title={title.getOrElse(null)}>{spans.flatMap(spanToXHTML)}</a>
        val withFollow =
          if (x.nofollow)
            base % new UnprefixedAttribute("nofollow", "true", new UnprefixedAttribute("target", "blank", Null))
          else base
        val result = x.kind match {
          case UrlKind.External => <i class="icon-globe"></i> ++ withFollow
          case UrlKind.Nonexistent => withFollow % new UnprefixedAttribute("class", "non-existent", Null)
          case _ => withFollow
        }
        result
    }
  }

  def imageLinkToXHTML( spans: Seq[Span], url: String, title: Option[String] )  = {
    urls(url) match {
      case None => Nil
      case Some(u) =>
        <img src={ u.url } title={ title.getOrElse(null) }
             alt={ spans.flatMap( spanToXHTML ) } ></img>
    }
  }

  def escapeURL( url : String ) : NodeSeq = {
    if ( url.startsWith( "mailto:" ) ) {
      val rand = new Random
      val mixed = url.flatMap { ch =>
        rand.nextInt(2) match {
          case 0 => "&#%d;".format(ch.toInt)
          case 1 => "&#x%x;".format(ch.toInt)
        }
      }
      Unparsed( mixed )
    } else {
      XMLText( url )
    }
  }
}

object Unescape {
  val escapeableChars = List( "\\", "`", "*", "_", "{", "}", "\\[", "\\]", "\\(", "\\)",
                              "#", "+", "\\-", "\\.", "!", ">" )

  val re = ("\\\\([" + escapeableChars.mkString + "])").r

  def unescape(source:String): String = {
    re.replaceAllIn(source, m => m.group(1))
  }
}

