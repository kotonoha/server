package com.fmpwizard.cometactor.pertab
package namedactor

import scala.xml.NodeSeq
import net.liftweb.http.{SessionVar, S}
import net.liftweb.common.Full
import net.liftweb.util.StringHelpers


/**
 * This trait adds a named comet actor on the page.
 * You should at least override cometClass to match the
 * class of your comet actor
 *
 * If all you want is different comet actors per tab, do not override
 * lazy val name.
 * If you want the same actor on some of the tabs, and you have a
 * specific value to group them, override lazy val name
 * override lazy val name= S.param("q")
 *
 *
 */
trait InsertNamedComet { self =>
  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass: String

  private lazy val savedName = new SessionVar[String](net.liftweb.util.Helpers.nextFuncName) {
    override protected def __nameSalt = self.getClass.getName
  }

  def name = savedName.is

  def messages = List[AnyRef]()

  def enabled = true

  final def render(xhtml: NodeSeq): NodeSeq = {
    if (enabled) {
      for (sess <- S.session) {
        messages foreach (
          sess.sendCometActorMessage(cometClass, Full(name), _)
          )
      }
      <lift:comet type={cometClass} name={name}>{xhtml}</lift:comet>
    } else {
      NodeSeq.Empty
    }
  }
}
