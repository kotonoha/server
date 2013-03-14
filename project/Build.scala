/*
* Copyright 2012 eiennohito
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

import coffeescript.Plugin.CoffeeKeys
import java.util.Date
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import sbt._
import inc.Analysis
import Keys._
import com.github.siasia.{PluginKeys => WPK, WebPlugin}


object Settings {

  val ourScalaVer = "2.10.1"

  val buildOrganization = "ws.kotonoha"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    scalaVersion := ourScalaVer
  )
}

object GitData {
  def gitVer = {
    val builder = new FileRepositoryBuilder();
    val repository = builder.setGitDir(file("./.git"))
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build();
    println("repository built")
    val head = repository.resolve("HEAD")
    println("head is " + head)
    val refwalk = new RevWalk(repository)
    val commit = refwalk.parseCommit(head)
    val time = new Date(commit.getCommitTime * 1000L)
    val id = head.abbreviate(10).name()
    (time, id)
  }
}

object JarExtraction {

}


object KotonohaBuild extends Build {
  import Settings._

  val gitId = TaskKey[String]("gitId", "Git commit id")
  val gitDate = TaskKey[Long]("gitDate", "Git commit date")

  val jsDir = SettingKey[File]("js-dir", "Javascript jar dir")
  val scriptOut = SettingKey[File]("script-out", "dir to be included in webapp")
  val scriptOutputDir = SettingKey[File]("sod", "dir to output js")
  val unzipJars = TaskKey[Seq[File]]("unzip-jars", "Unzip jars with js files inside")
  val compileJs = config("compilejs")

  val gitdata = {
    val (time, id) = GitData.gitVer
    (
      gitId := id,
      gitDate := time.getTime
    )
  }


  lazy val kotonoha = Project(
    id = "kotonoha",
    base = file("."),
    settings = buildSettings ++ Seq(gitdata._1) ++ Seq(gitdata._2) ++
      coffeescript.Plugin.coffeeSettingsIn(compileJs) ++
      WebPlugin.webSettings ++
      Seq(
      jsDir := file("jslib"),
      scriptOut <<= (target in Compile).apply(_ / "javascript" ),
      scriptOutputDir <<= scriptOut apply (_ / "static"),
      (sourceDirectory in compileJs) <<= sourceDirectory in Compile,
      (resourceManaged in (compileJs, CoffeeKeys.coffee)) <<= scriptOutputDir,
      (WPK.webappResources in Compile) <+= scriptOut,
      unzipJars <<= (jsDir, scriptOutputDir) map {
        (jsd, so) =>
          (jsd  * (GlobFilter("*.jar"))) flatMap { f => IO.unzip(f, so) } get
      },
      compile in compileJs := Analysis.Empty,
      resourceGenerators in compileJs := Nil,
      compile in Compile <<= (compile in Compile).dependsOn(CoffeeKeys.coffee in compileJs),
      copyResources in Compile <<= (copyResources in Compile).dependsOn(unzipJars)
    )
  ) dependsOn(model, akane)

  lazy val model = Project(
    id = "model",
    base = file("model"),
    settings = buildSettings
  )
  
  lazy val akane = Project(
    id = "akane",
    base = file("akane"),
    settings = buildSettings
  )

}
