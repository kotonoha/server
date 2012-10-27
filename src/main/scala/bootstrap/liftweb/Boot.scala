package bootstrap.liftweb

/**
 * @author eiennohito
 * @since 07.10.11 
 */

import net.liftweb._
import common.Full
import http.Html5Properties
import sitemap.Loc.If
import util._
import Helpers._

import common._
import http._
import provider.HTTPCookie
import sitemap._
import Loc._
import ws.kotonoha.server.actors.ReleaseAkkaMain
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.UserRecord
import ws.kotonoha.server.web.rest._
import model.Words
import ws.kotonoha.server.actors.lift.Ping
import com.weiglewilczek.slf4s.Logging
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import ws.kotonoha.server.web.snippet.ClasspathResource
import java.net.URI


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logging {
  def boot {
    MongoDbInit.init()
    RegisterJodaTimeConversionHelpers.register()

    //Bootstrap lazy akka
    ReleaseAkkaMain.root ! Ping

    // where to search snippet
    LiftRules.addToPackages("ws.kotonoha.server.web")

    val loggedin = If(UserRecord.loggedIn_? _, "You are not logged in")
    // Build SiteMap
    val admin = Menu.i("Admin") / "admin" / "index" >> If(() => UserRecord.isAdmin, "Only administrators allowed here") submenus(
      Menu.i("Client List") / "admin" / "clients",
      Menu.i("Access list") / "admin" / "access",
      Menu.i("Configuration") / "admin" / "config",
      Menu.i("Debug") / "admin" / "debug",
      Menu.i("Global learning") / "admin" / "learning"
    )

    def sitemap = {
      SiteMap(
        Menu.i("Home") / "index",
        Menu.i("Client Authorizations") / "user" / "tokens" >> loggedin >> UserRecord.AddUserMenusAfter,
        Menu.i("Learning") / "learning" / "index" >> loggedin submenus(
          Menu.i("Repetition") / "learning" / "repeat",
          Menu.i("Scheduled words") / "learning" / "scheduled_cnt",
          Menu.i("OF Matrix") / "learning" / "ofmatrix",
          Menu.i("Words for review") / "learning" / "bad_cards"
          ),
        admin,
        Menu.i("Words") / "words" / "index" >> loggedin submenus(
          Menu.i("Add") / "words" / "add",
          Menu.i("Approve & Review") / "words" / "approve_added",
          Menu.i("Detail") / "words" / "detail" >> Hidden
          ),
        Menu.i("Tools") / "tools" / "index" submenus(
          Menu.i("Test parser") / "tools" / "parser",
          Menu.i("Comet test") / "tools" / "comet_test",
          Menu.i("JMDict") / "tools" / "jmdict",
          Menu.i("Warodai") / "tools" / "warodai",
          Menu.i("Examples") / "tools" / "examples",
          Menu.i("Stroke orders") / "tools" / "kakijyun",
          Menu.i("Additional") / "tools" / "addit_info",
          Menu.i("Sandbox") / "tools" / "sandbox" >> If(() => Props.devMode, "Inaccessible")
          )
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

    LiftRules.statelessDispatchTable.append(Learning)
    LiftRules.statelessDispatchTable.append(QrRest)
    LiftRules.statelessDispatchTable.append(new StatusApi)

    LiftRules.dispatch.append(Stats)
    LiftRules.dispatch.append(Grants)
    LiftRules.dispatch.append(Juman)
    LiftRules.dispatch.append(PersonalStats)
    LiftRules.dispatch.append(Words)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

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
      new Html5Properties(r.userAgent))

    // Make a transaction span the whole HTTP request
    //S.addAround(DB.buildLoanWrapper)

    LiftRules.unloadHooks.append({ () => ReleaseAkkaMain.shutdown() })

    LiftRules.snippetDispatch.append(
      Map("cpres" -> ClasspathResource)
    )

    //transform njs to ws/kotonoha/script/angular
//    ResourceServer.rewrite({
//      case "njs" :: xs => ".." :: "ws" :: "kotonoha" :: "script" :: "angular" :: xs
//      case "cpres" :: xs => ".." :: xs
//      case x if x.last.startsWith("glyphicons-halflings") => x
//    })
//
//    LiftRules.getResource = name => {
//      val r = LiftRules.defaultGetResource(name)
//      r or (try {
//        val uri = new URI(name)
//        val path = uri.normalize().getPath
//        val x = LiftRules.getClass.getResource(path)
//        Box !! x
//      } catch {
//        case _ => Empty
//      })
//    }
//
//    //allow css, js and png from classpath
//    ResourceServer.allow({
//      case x if x.last.endsWith(".css") => true
//      case x if x.last.endsWith(".js") => true
//      case x if x.last.endsWith(".png") => true
//    })
  }
}
