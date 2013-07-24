package ws.kotonoha.server.web.snippet

import xml.NodeSeq
import net.liftweb.http.S
import ws.kotonoha.server.records.{UserRecord, ClientRecord}
import net.liftweb.common.Full
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.actors.{EncryptedTokenString, CreateToken}
import org.joda.time.format.DateTimeFormat
import ws.kotonoha.server.util.Formatting
import scala.concurrent.Await
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 11.12.12 
 */

object OauthRequest extends Akka with ReleaseAkka {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import concurrent.duration._

  def dateRenderer = {
    val r = DateTimeFormat.forPattern("'browser request on 'yyyy.MM.dd HH:mm")
    r.withZone(Formatting.requestFormatter.is.tz)
  }

  def render(in: NodeSeq): NodeSeq = {
    val key = S.param("key")
    key.flatMap(k => ClientRecord where (_.apiPublic eqs (k)) get()) match {
      case Full(cl) => {
        UserRecord.currentId match {
          case Full(u) => {
            val str: String = makeAuthString(u, cl)
            <p>
              You are trying to authentificate
              {cl.name.is}<a class="btn" href={"http://kotonoha.ws/intent/auth?data=" + str}>Confirm</a>
            </p>
          }
          case _ => {
            val emb = S.param("email")
            val pwdb = S.param("passwd")
            (emb, pwdb) match {
              case (Full(eml), pwd) if eml.length > 1 => {
                val uBox = UserRecord.checkUser(Full(eml), pwd)
                uBox match {
                  case Full(u) => {
                    val msg = makeAuthString(u.id.is, cl)
                    S.redirectTo("kotonoha://kotonoha.ws/intent/auth?data=" + msg)
                  }
                  case _ => {
                    <b>Invalid username or password, please try one more time</b> ++
                      in
                  }
                }
              }
              case _ => {
                in
              }
            }
          }
        }
      }
      case _ => <b>Invalid client, sry pal!</b>
    }
  }

  def makeAuthString(uid: ObjectId, cl: ClientRecord): String = {
    import ws.kotonoha.server.actors.UserSupport._
    val name = dateRenderer.print(System.currentTimeMillis())
    val f = (akkaServ ? EncryptedTokenString(CreateToken(uid, name), cl.apiPrivate.is).u(uid))
      .mapTo[String]
    val str = Await.result(f, 1 minute)
    str
  }
}
