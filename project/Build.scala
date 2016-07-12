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
import com.earldouglas.xsbtwebplugin.{WebPlugin, PluginKeys => WPK}
import sbtbuildinfo.Plugin._


object Common {

  val ourScalaVer = "2.11.8"

  val buildOrganization = "ws.kotonoha"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := buildOrganization,
    scalaVersion := ourScalaVer,
    version := "0.2-SNAPSHOT"
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
    println("head is " + head.name())
    val refwalk = new RevWalk(repository)
    val commit = refwalk.parseCommit(head)
    val time = new Date(commit.getCommitTime * 1000L)
    val id = head.name()
    (time, id)
  }
}


object Kotonoha {

  val gitId = TaskKey[String]("gitId", "Git commit id")
  val gitDate = TaskKey[Long]("gitDate", "Git commit date")

  val jsDir = SettingKey[File]("js-dir", "Javascript jar dir")
  val scriptOut = SettingKey[File]("script-out", "dir to be included in webapp")
  val scriptOutputDir = SettingKey[File]("sod", "dir to output js")
  val unzipJars = TaskKey[Seq[File]]("unzip-jars", "Unzip jars with js files inside")
  val compileJs = config("compilejs")

  lazy val gitSettings = {
    val (time, id) = GitData.gitVer
    Seq(
      gitId := id,
      gitDate := time.getTime
    )
  }

  lazy val binfo = sbtbuildinfo.Plugin.buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, gitId, gitDate),
    buildInfoPackage := "ws.kotonoha.server.util",
    buildInfoObject := "BuildInfo"
  )

  lazy val scalatest = Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )


  val liftVersion = "2.6.3.di"
  // Put the current/latest lift version here
  val akkaVer = "2.4.8"
  val rogueVer = "2.5.0"

  val liftDeps = Seq(
    "net.liftweb" %% "lift-util" % liftVersion exclude("joda-time", "joda-time"),
    "net.liftweb" %% "lift-json-ext" % liftVersion exclude("joda-time", "joda-time"),
    "net.liftweb" %% "lift-webkit" % liftVersion,
    "net.liftweb" %% "lift-wizard" % liftVersion,
    "net.liftweb" %% "lift-mongodb-record" % liftVersion exclude("org.mongodb", "mongo-java-driver"),
    "net.liftweb" %% "lift-json-scalaz7" % liftVersion exclude("org.scalaz", "scalaz-core_2.9.1"),
    "net.liftmodules" %% "oauth_2.6" % "1.2-SNAPSHOT",
    "net.liftweb" %% "lift-testkit" % liftVersion % "test",
    "javax.servlet" % "servlet-api" % "2.5" % "provided")

  val akkaDeps = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVer,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVer,
    "com.typesafe.akka" %% "akka-testkit" % akkaVer % "test"
  )

  val rogueDeps = Seq(
    "com.foursquare" %% "rogue-lift" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-field" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-index" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-core" % rogueVer intransitive()
  )

  val kotonohaRestDeps = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",

    "net.codingwell" %% "scala-guice" % "4.0.1",

    "org.scalaz" %% "scalaz-core" % "7.2.0",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "javax.transaction" % "jta" % "1.0.1B" % "provided",

    "org.mongodb" %% "casbah" % "2.8.2" exclude("org.specs2", "*"),

    "net.java.sen" % "lucene-gosen" % "2.1" exclude("org.slf4j", "slf4j-jdk14"),
    "com.google.zxing" % "javase" % "3.2.1",
    "org.bouncycastle" % "bcprov-jdk15on" % "1.54",

    "com.j256.ormlite" % "ormlite-jdbc" % "4.42" % "test",
    "org.xerial" % "sqlite-jdbc" % "3.6.16" % "test",

    "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
  )

  lazy val kotonohaSettings =
    gitSettings ++
      binfo ++
      coffeescript.Plugin.coffeeSettingsIn(compileJs) ++
      WebPlugin.webSettings ++
      scalatest ++ Seq(
      name := "server",
      parallelExecution in Test := false,
      javacOptions ++= Seq("-encoding", "utf8"),
      scalacOptions ++= Seq("-unchecked", "-language:postfixOps"),

      jsDir := file("jslib"),
      scriptOut <<= (target in Compile).apply(_ / "javascript"),
      scriptOutputDir <<= scriptOut apply (_ / "static"),
      (sourceDirectory in compileJs) <<= sourceDirectory in Compile,
      (resourceManaged in(compileJs, CoffeeKeys.coffee)) <<= scriptOutputDir,
      (WPK.webappResources in Compile) <+= scriptOut,
      unzipJars <<= (jsDir, scriptOutputDir) map {
        (jsd, so) =>
          (jsd * GlobFilter("*.jar")) flatMap { f => IO.unzip(f, so) } get
      },
      compile in compileJs := Analysis.Empty,
      resourceGenerators in compileJs := Nil,
      compile in Compile <<= (compile in Compile).dependsOn(CoffeeKeys.coffee in compileJs),
      copyResources in Compile <<= (copyResources in Compile).dependsOn(unzipJars),

      resolvers += "eiennohito's repo" at "http://eiennohito.github.com/maven/",
      libraryDependencies ++= (liftDeps ++ akkaDeps ++ rogueDeps ++ kotonohaRestDeps)
    )

  val modelDeps = Seq(
    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2",

    "com.google.code.gson" % "gson" % "2.2.2",
    "com.j256.ormlite" % "ormlite-core" % "4.43",
    "org.scribe" % "scribe" % "1.3.3"
  )

  lazy val modelSettings = Seq(
    name := "kotonoha-model",
    crossPaths := false,
    libraryDependencies ++= modelDeps
  )
}
