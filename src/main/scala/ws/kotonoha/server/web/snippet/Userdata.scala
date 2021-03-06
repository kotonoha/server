/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.web.snippet

import java.util.Locale

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.Full
import net.liftweb.http.js.{JE, JsCmd, JsCmds}
import net.liftweb.http.rest.RestContinuation
import net.liftweb.http.{InternalServerErrorResponse, JsonResponse, S}
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonParser
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import ws.kotonoha.akane.dic.lucene.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.lift.json.{JLCaseClass, JLift, JRead}
import ws.kotonoha.server.actors.dict.JMDictTransforms
import ws.kotonoha.server.actors.model.DictCard
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.lang.Iso6392
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.UserRecord
import ws.kotonoha.server.util.{Jobj, LangUtil}
import ws.kotonoha.server.web.lift.Binders.NodeSeqFn

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2017/02/13
  */

class UserOps @Inject()(
  rm: RMData
)(implicit ec: ExecutionContext) {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def usernameAvailable(name: String): Future[Boolean] = {
    val q = UserRecord.where(_.username eqs name)
    rm.count(q).map(_ == 0)
  }
}

object JMDictLanguageStat {
  implicit val jwrite = JLCaseClass.format[JMDictLanguageStat]
}

case class JMDictLanguageStat(code: String, name: String, entries: Long, glosses: Long, selected: Boolean)

class Userdata @Inject()(
  uc: UserContext,
  uop: UserOps,
  jmd: LuceneJmdict
)(implicit ec: ExecutionContext) extends StrictLogging {

  import ws.kotonoha.server.web.lift.Binders._

  def changePassword(in: NodeSeq): NodeSeq = {
    val oldpass = S.param("old-pass").openOr("")
    val newpass = S.param("new-pass").openOr("")
    val newpass2 = S.param("new-pass2").openOr("")

    val user = UserRecord.currentUser.openOrThrowException("the user should be logged in to be here!")

    val newPassTransform =
      "@new-pass [value]" #> newpass &
        "@new-pass2 [value]" #> newpass2

    val oldpassTransform = "@old-pass [value]" #> oldpass

    val tf = if (oldpass == "" && newpass == "" && newpass2 == "") {
      oldpassTransform
    } else if (!user.password.isMatch(oldpass)) {
      newPassTransform &
        "#pwd-notices *" #> <span class="alert alert-warning">Old password is invalid.</span>
    } else if (!newpass.equals(newpass2)) {
      oldpassTransform &
        "#pwd-notices *" #> <span class="alert alert-warning">New password does not match its confirmation.</span>
    } else if (newpass.length < 6) {
      oldpassTransform &
        "#pwd-notices *" #> <span class="alert alert-warning">New password should be longer than 6 characters</span>
    } else {
      user.password.setPassword(newpass)
      user.save()
      logger.debug(s"user ${uc.uid} has changed password")
      "#pwd-notices *" #> <span class="alert alert-success">New password has been set!</span>
    }

    tf(in)
  }

  private def locales(): JValue = {
    val all = Locale.getAvailableLocales

    val data = all.view.map { m =>
      Jobj(
        "key" -> m.toLanguageTag,
        "name" -> m.getDisplayName
      )
    }
    JArray(data.toList)
  }

  private def timezones(): JValue = {
    val all = DateTimeZone.getAvailableIDs.asScala
    JArray(all.toList.sorted.map(s => Jobj("name" -> s)))
  }

  def testTimezoneLocale(tzid: String, locale: String) = {
    val tz = DateTimeZone.forID(tzid)
    val date = DateTime.now(tz)


    val locobj = Locale.forLanguageTag(locale)
    val fmtr = DateTimeFormat.shortDateTime().withLocale(locobj)

    val utcfmt = fmtr.withZoneUTC()
    val localfmt = fmtr.withZone(tz)

    Jobj(
      "local" -> localfmt.print(date),
      "utc" -> utcfmt.print(date),
      "offset" -> tz.getOffset(date)
    )
  }

  def checkUsernameBack(user: UserRecord, name: String): Future[Boolean] = {
    if (user.username.value == name) {
      Future.successful(true)
    } else {
      uop.usernameAvailable(name)
    }
  }

  def checkUsername(user: UserRecord, name: String): Future[JValue] = {
    checkUsernameBack(user, name).map {
      case true => Jobj("status" -> "ok")
      case false => Jobj("status" -> "error", "message" -> s"Username $name is already taken")
    }
  }

  private def jmdictLanguages(selected: Set[String]): Seq[JMDictLanguageStat] = {
    val langs = jmd.info.langs
    val ordered = langs.freqs.sortBy(-_.entryCnt)
    ordered.map { lf =>
      ///val loc = Locale.
      val code = lf.language
      JMDictLanguageStat(
        code,
        Iso6392.byBib.get(code).map(_.englishName).getOrElse(code),
        lf.entryCnt,
        lf.glossCnt,
        selected.contains(code)
      )
    }
  }

  def jmdictEntry(langs: Seq[String] = LangUtil.langsFor(uc.settings)): JValue = {
    val query = JmdictQuery(limit = 1, langs = langs, other = Seq(JmdictQueryPart("*")))
    jmd.find(query).data.headOption match {
      case Some(e) =>
        val entry = JMDictTransforms.toEntry(e, langs.toSet)
        val card = DictCard.makeCard(entry)
        JLift.write(card)
      case None => JNothing
    }
  }

  def userPipeline(in: NodeSeq): NodeSeq = {
    val user = UserRecord.currentUser.openOrThrowException("there should be a logged in user here")

    val fields = List(user.email, user.firstName, user.lastName, user.username, user.locale, user.timezone)

    val json = JObject(fields.map(f => JField(f.name, f.asJValue)) ++ Seq(
      JField("locales", locales()),
      JField("timezones", timezones()),
      JField("timezoneTest", testTimezoneLocale(user.timezone.value, user.locale.get)),
      JField("languages", JLift.write(jmdictLanguages(LangUtil.acceptableFor(uc.settings)))),
      JField("jmdict", jmdictEntry())
    ))

    val jexp = JsCmds.SetExp(JE.JsVar("window", "currentUserObj"), json)

    val ret = Jcall.handler {
      case JCommand(cmd) =>
        cmd match {
          case RecomputeCmd.JVal(x) =>
            testTimezoneLocale(x.timezone, x.locale)
          case c if c.name == "save-user" =>
            Jcall.async { saveUser(user, c) }
          case RecomputeCmd.CheckUsername(name) =>
            Jcall.async(checkUsername(user, name))
          case RecomputeCmd.UpdateJmdict(langs) =>
            jmdictEntry(langs)
          case _ => JNothing
        }
      case x: JValue =>
        logger.debug(s"$x!")
        JInt(123)
    }

    val jreply = ret.ngService("UserCallback")
    S.appendGlobalJs(jexp)
    S.appendGlobalJs(jreply)
    NodeSeq.Empty
  }

  private def saveUser(user: UserRecord, c: JCommand) = {
    val username = c.data \ "username" match {
      case JString(s) => s
      case _ => ""
    }
    checkUsernameBack(user, username).map {
      case false =>
        Jobj("status" -> "failure", "message" -> s"Username $username is already occupied")
      case true =>
        user.setFieldsFromJValue(c.data)
        user.save()

        JLift.read[List[String]](c.data \ "languages") match {
          case Full(langs) =>
            val sets = uc.settings
            sets.dictLangs.set(langs)
            sets.save()
          case _ =>
        }

        Jobj("status" -> "ok")
    }
  }
}

object UserObj {
  import ws.kotonoha.server.web.lift.Binders._

  def username: NodeSeqFn = {
    "* *" #> UserRecord.currentUser.map(_.username.value).filter(_.length > 0).openOr("Profile")
  }
}

object RecomputeCmd {
  implicit val jread = JLCaseClass.read[RecomputeCmd]

  object JVal extends JCommand.Unapplier[RecomputeCmd]("recompute")

  object CheckUsername extends JCommand.Unapplier[String]("check-username")
  object UpdateJmdict extends JCommand.Unapplier[Seq[String]]("update-jmdict")

}

case class RecomputeCmd(locale: String, timezone: String)

case class JCommand(name: String, data: JValue)

object JCommand {

  class Unapplier[T](val name: String)(implicit read: JRead[T]) {
    def unapply(cmd: JCommand): Option[T] = {
      if (cmd.name == name) {
        read.read(cmd.data).toOption
      } else None
    }
  }

  def unapply(arg: JValue): Option[JCommand] = {
    arg \ "cmd" match {
      case JString(cmd) => Some(JCommand(cmd, arg \ "data"))
      case _ => None
    }
  }
}

object Jcall extends StrictLogging {

  def async(f: => Future[JValue])(implicit ec: ExecutionContext): Nothing = {
    RestContinuation.async { reply =>
      f.onComplete {
        case scala.util.Success(jv) =>
          reply(JsonResponse(jv))
        case scala.util.Failure(ex) =>
          logger.error(s"error in async call", ex)
          reply(InternalServerErrorResponse())
      }
    }
  }

  def handler(handler: PartialFunction[JValue, JValue])(implicit file: sourcecode.File, codeline: sourcecode.Line): Jcall = {

    val key = S.generateFuncName

    S.functionLifespan(true) {
      S.addFunctionMap(key, (data: List[String]) => {
        for {
          line <- data
          parsed <- JsonParser.parseOpt(line)
        } yield {
          if (handler.isDefinedAt(parsed)) {
            handler(parsed)
          } else {
            logger.error(s"json async call defined on ${file.value}:${codeline.value} was not handled")
            InternalServerErrorResponse()
          }
        }
      })
    }

    new Jcall(key)
  }
}

class Jcall(key: String) {
  private val methodBody = {
    s"""
       |{
       |var d=jQuery.Deferred();
       |lift.ajax("$key="+encodeURIComponent(JSON.stringify(obj)),function(succ){d.resolve(succ);},function(fail){d.resolve(fail)},"json");
       |return d.promise();
       |}
     """.stripMargin.replace("\n", "")
  }

  def method(name: String): JsCmd = ???

  def ngService(name: String): JsCmd = {
    val snippet =
      s"""
         |angular.module("kotonoha").service("$name", function () { return function(obj) $methodBody });
       """.stripMargin
    JsCmds.Run(snippet)
  }
}


