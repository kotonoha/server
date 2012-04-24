package bootstrap.liftweb

/**
 * @author eiennohito
 * @since 07.10.11 
 */

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import org.eiennohito.kotonoha.actors.ReleaseAkkaMain
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.UserRecord
import org.eiennohito.kotonoha.web.rest.{StatusApi, QrRest, Learning, SimpleRest}


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    MongoDbInit.init()

    // where to search snippet
    LiftRules.addToPackages("org.eiennohito.kotonoha.web")

    val loggedin = If(UserRecord.loggedIn_? _, "You are not logged in")
    // Build SiteMap
    def sitemap = SiteMap(
      Menu.i("Home") / "index",
      Menu.i("Client Authorizations") / "user" / "tokens" >> loggedin >> UserRecord.AddUserMenusAfter,
      Menu.i("Learning") / "learning" / "index" >> loggedin submenus (
        Menu.i("Scheduled words") / "learning" / "scheduled_cnt",
        Menu.i("OF Matrix") / "learning" / "ofmatrix"
        ),
      Menu.i("Admin") / "admin" / "index" >> If(() => UserRecord.isAdmin, "Only administrators allowed here") submenus (
          Menu.i("Client List") / "admin" / "clients",
          Menu.i("Configuration") / "admin" / "config"
        ),
      Menu.i("Words") / "words" / "index" >> loggedin submenus (
          Menu.i("Add") / "words" / "add",
          Menu.i("Approve") / "words" / "approve_added" >> Hidden,
          Menu.i("Detail") / "words" / "detail" >> Hidden
        ),
      Menu.i("Tools") / "tools" / "index" submenus (
        Menu.i("Test parser") / "tools" / "parser",
        Menu.i("Comet test") / "tools" / "comet_test",
        Menu.i("JMDict") / "tools" / "jmdict",
        Menu.i("Warodai") / "tools" / "warodai",
        Menu.i("Examples") / "tools" / "examples"
      )
    )

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
    //LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    // Make a transaction span the whole HTTP request
    //S.addAround(DB.buildLoanWrapper)

    LiftRules.unloadHooks.append({ () => ReleaseAkkaMain.shutdown() })
  }
}
