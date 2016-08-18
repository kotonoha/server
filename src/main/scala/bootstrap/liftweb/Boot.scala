/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package bootstrap.liftweb

/**
 * @author eiennohito
 * @since 07.10.11 
 */

import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.actor.{ILAExecute, LAScheduler}
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap._
import net.liftweb.util._
import ws.kotonoha.server.KotonohaConfig
import ws.kotonoha.server.actors.lift.Ping
import ws.kotonoha.server.actors.{InitUsers, ReleaseAkkaMain}
import ws.kotonoha.server.ioc.{KotonohaIoc}
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.UserRecord
import ws.kotonoha.server.web.lift.{SnippetResolver, SnippetResolverConfig}
import ws.kotonoha.server.web.loc.{NewsLoc, WikiLoc}
import ws.kotonoha.server.web.rest._
import ws.kotonoha.server.web.rest.admin.{OFHistory, Stats}
import ws.kotonoha.server.web.rest.model.{Cards, Words}
import ws.kotonoha.server.web.snippet.{CdnSnippet, ClasspathResource, ModeSnippet}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logging {

  def checkAdiminAccount() = {
    import ws.kotonoha.server.util.KBsonDSL._
    val admins = UserRecord.count("superUser" -> true)
    if (admins == 0) {
      val rec = UserRecord.createRecord
      val email = KotonohaConfig.safeString("admin.email").getOrElse("admin@(none)")
      rec.email(email)
      rec.password.setPassword(KotonohaConfig.safeString("admin.password").getOrElse("admin"))
      rec.superUser(true)
      rec.validated(true)
      try {
        rec.save()
        logger.info(s"Created a new superUser with email $email")
      } catch {
        case NonFatal(t) => logger.error("There was no admin account present and creating it failed", t)
      }
    }
  }

  def boot() = {

    LiftRules.logUnreadRequestVars = true
    LiftRules.updateAsyncMetaList(_ => List(new Servlet31AsyncProviderMeta))

    logger.info(s"run mode: ${Props.mode}")

    val config = KotonohaConfig.config

    val ioc = new KotonohaIoc(config)

    configureInjection(ioc)

    MongoDbInit.init()

    LiftRules.unloadHooks.append(() => ioc.close())
    LiftRules.unloadHooks.append(() => MongoDbInit.stop())

    checkAdiminAccount()

    /*val c = Pointer.allocateByte()
    c.set(0.toByte)
    val m = MecabLibrary.mecab_new2(c)
    MecabLibrary.mecab_destroy(m)*/

    //MecabInit.init()
    //LiftRules.unloadHooks.append(() => MecabInit.unload())

    //Bootstrap lazy akka
    ReleaseAkkaMain.init(ioc)
    ReleaseAkkaMain.global ! Ping
    ReleaseAkkaMain.global ! InitUsers

    //TODO: fix CSR
    LiftRules.securityRules = () => SecurityRules(https = None, content = None)

    val ec = ioc.spawn[ExecutionContext]

    val liftex = new ILAExecute {
      def shutdown() {}

      def execute(f: () => Unit) {
        ec.execute(new Runnable {
          def run() = { f() }
        })
      }
    }

    Schedule.threadPoolSize = 1
    Schedule.maxThreadPoolSize = 16
    LAScheduler.createExecutor = () => liftex


    // where to search snippet
    LiftRules.addToPackages("ws.kotonoha.server.web")

    val loggedin = If(UserRecord.loggedIn_? _, "You are not logged in")
    // Build SiteMap
    def admin = Menu.i("Admin") / "admin" / "index" >> If(() => UserRecord.isAdmin, "Only administrators allowed here") submenus(
      Menu.i("Client List") / "admin" / "clients",
      Menu.i("Access list") / "admin" / "access",
      Menu.i("Configuration") / "admin" / "config",
      Menu.i("Debug") / "admin" / "debug",
      Menu.i("Global learning") / "admin" / "learning",
      Menu.i("OF history") / "admin" / "ofhistory",
      Menu.i("Load resources") / "admin" / "resources"
      )

    val emptyLink: LinkText[Unit] = LinkText(_ => Nil)
    val loc = Loc("static", Link(List("static"), true, "/static"), emptyLink , Hidden)
    val static = Menu.apply(loc)

    def sitemap = {
      SiteMap(
        Menu.i("Home") / "index" submenus(
          Menu.i("Mobile Login") / "user" / "tokens" >> loggedin >> UserRecord.AddUserMenusAfter
          ),
        Menu.i("Learning") / "learning" / "index" >> loggedin submenus(
          Menu.i("Repetition") / "learning" / "repeat",
          Menu.i("OF Matrix") / "learning" / "ofmatrix",
          Menu.i("Words for review") / "learning" / "bad_cards",
          Menu.i("Tags") / "learning" / "tags",
          Menu.i("History") / "learning" / "history"
          ),
        admin,
        Menu.i("Words") / "words" / "index" >> loggedin submenus(
          Menu.i("Add") / "words" / "add",
          Menu.i("Approve & Review") / "words" / "approve_added",
          Menu.i("Detail") / "words" / "detail" >> Hidden
          ),
        Menu.i("Wiki") / "wiki",
        Menu.i("Tools") / "tools" / "index" submenus(
          Menu.i("Test parser") / "tools" / "parser",
          Menu.i("JMDict") / "tools" / "jmdict",
          Menu.i("Warodai") / "tools" / "warodai",
          Menu.i("Examples") / "tools" / "examples",
          Menu.i("Stroke orders") / "tools" / "kakijyun",
          Menu.i("Kanji") / "tools" / "kanji" >> If(() => Props.devMode, "Not debug"),
          Menu.i("Last wiki edits") / "tools" / "wikiedits",
          Menu.i("Depencency Tree") / "tools" / "knp" >> If(() => Props.devMode, "Not avaliable"),
          Menu.i("Sandbox") / "tools" / "sandbox" >> If(() => Props.devMode, "Inaccessible")
      ),
        Menu.i("Oauth") / "oauth" >> Hidden submenus (
          Menu.i("OAuth request") / "oauth" / "request"
      ),
        static,
        Menu(new WikiLoc),
        Menu(new NewsLoc)
      )
    }

    // more complex because this menu allows anything in the
    // /static path to be visible
    //Menu(Loc("Static", Link(List("static"), true, "/static/index"),
    //	       "Static Content")))

    def sitemapMutators = UserRecord.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

    LiftRules.dispatch.append(Learning)
    LiftRules.dispatch.append(QrRest)
    LiftRules.dispatch.append(new StatusApi)

    LiftRules.dispatch.append(Words)
    LiftRules.dispatch.append(Stats)
    LiftRules.dispatch.append(Grants)
    LiftRules.dispatch.append(Cards)
    LiftRules.dispatch.append(Juman)

    LiftRules.dispatch.append(OFHistory)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => UserRecord.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      if (r.path(0) != "static") {
        Html5Properties(r.userAgent)
      } else {
        Html5StaticProperties(r.userAgent)
      }
    )

    LiftRules.statelessReqTest.append({
      case StatelessReqTest("static" :: _, _) => true
      //case StatelessReqTest(_, r) => r.cookies.exists(_.name == UserRecord.authCookie)
    })

    // Make a transaction span the whole HTTP request
    //S.addAround(DB.buildLoanWrapper)
  }

  private def configureInjection(ioc: KotonohaIoc) = {
    val rcfg = new SnippetResolverConfig
    rcfg.shortcut("cpres", ClasspathResource)
    rcfg.shortcut("mode", ModeSnippet)
    rcfg.shortcut("cdn", CdnSnippet)
    val res = new SnippetResolver(ioc.injector, rcfg)
    LiftRules.snippets.append(res)
    S.addAround(res.wrapUser())
    LiftRules.cometCreationFactory.default.set(res.cometCreation())
  }
}
