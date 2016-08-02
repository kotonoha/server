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


import java.util.Date

import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.irundaia.sass.Maxified
import org.irundaia.sbt.sass.SbtSassify.autoImport.SassKeys
import sbt.Keys._
import sbt._
import sbt.inc.Analysis
import sbtbuildinfo.BuildInfoKeys._


object Common {

  val ourScalaVer = "2.11.8"

  val buildOrganization = "ws.kotonoha"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := buildOrganization,
    scalaVersion := ourScalaVer,
    version := "0.2-SNAPSHOT",
    resolvers += "kyouni" at "http://lotus.kuee.kyoto-u.ac.jp/nexus/content/groups/public/",
    excludeDependencies ++= Seq(
      SbtExclusionRule("net.liftweb"),
      SbtExclusionRule("javax.transaction")
    )
  ) ++ scalacSupport

  lazy val scalacSupport = Seq(
    scalacOptions ++= {
      val base = if (scalaVersion.value.startsWith("2.11.8")) {
        Seq(
          "-Ybackend:GenBCode",
          "-Yopt:l:classpath",
          "-Yopt-warnings"
        )
      } else Seq.empty
      Seq(
        "-target:jvm-1.8",
        "-feature",
        "-deprecation"
      ) ++ base
    },
    scalacOptions in Compile ++= (if (scalaVersion.value.startsWith("2.11.8")) {
      Seq("-Ydelambdafy:method")
    } else {
      Seq.empty
    }),
    scalacOptions in Test ++= (if (scalaVersion.value.startsWith("2.11.8")) {
      Seq("-Ydelambdafy:inline")
    } else {
      Seq.empty
    })
  ) ++ compatSettings ++ {
    if (ourScalaVer.endsWith("-SNAPSHOT")) {
      resolvers += Resolver.sonatypeRepo("snapshots")
    } else Seq.empty
  }

  private lazy val compatSettings = {
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.11")) {
        Seq("org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.7.0")
      } else Seq.empty
    }
  }

}

object GitData {
  def gitVer = {
    val builder = new FileRepositoryBuilder();
    val repository = builder.setGitDir(file("./.git"))
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build()

    val head = repository.resolve("HEAD")
    println("head is " + head.name())
    val refwalk = new RevWalk(repository)
    val commit = refwalk.parseCommit(head)
    val time = new Date(commit.getCommitTime * 1000L)
    val id = head.name()
    (time, id)
  }
}

object Pbuf {

  import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

  lazy val scalaPbVersion = "0.5.32"
  lazy val grpcVersion = "0.14.1"

  def pbScala(): Seq[Setting[_]] = {
    val config = PB.protobufSettings ++ Seq(
      PB.flatPackage in PB.protobufConfig := true,
      PB.javaConversions in PB.protobufConfig := true,
      PB.scalapbVersion := scalaPbVersion,
      PB.runProtoc in PB.protobufConfig := (args =>
        com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))
    )

    val runtimeDep =
      libraryDependencies ++= Seq(
        "com.trueaccord.scalapb" %% "scalapb-runtime" % scalaPbVersion % PB.protobufConfig,
        "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
        "io.grpc" % "grpc-stub" % grpcVersion,
        "io.grpc" % "grpc-core" % grpcVersion,
        "io.grpc" % "grpc-netty" % grpcVersion
      )

    config ++ Seq(
      runtimeDep
    )
  }

  def protoIncludes(files: Project*) = {
    val paths = files.map(f => f.base / "src" / "main" / "protobuf")
    Seq(PB.includePaths in PB.protobufConfig ++= paths)
  }
}


object Kotonoha {

  val gitId = TaskKey[String]("gitId", "Git commit id")
  val gitDate = TaskKey[Long]("gitDate", "Git commit date")

  val jsDir = SettingKey[File]("js-dir", "Javascript jar dir")
  val scriptOutputDir = SettingKey[File]("sod", "dir to output js")
  val unzipJars = TaskKey[Seq[File]]("unzip-jars", "Unzip jars with js files inside")
  val compileJs = config("compilejs")

  val postsMdFiles = SettingKey[Seq[String]]("news-md-files", "Files for news")

  lazy val gitSettings = {
    val (time, id) = GitData.gitVer
    Seq(
      gitId := id,
      gitDate := time.getTime
    )
  }

  lazy val binfo = Def.settings(
    buildInfoKeys := Seq(version, scalaVersion, sbtVersion, gitId, gitDate, postsMdFiles),
    buildInfoPackage := "ws.kotonoha.server.util",
    buildInfoObject := "BuildInfo"
  )

  lazy val scalatest = Seq(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.6" % Test,
      "org.scalacheck" %% "scalacheck" % "1.12.5" % Test
    )
  )


  val liftPackage = "ws.kotonoha.liftweb"
  val liftVersion = "2.6.3.di"
  val akkaVer = "2.4.8"
  val rogueVer = "2.5.0"
  val luceneVersion = "6.1.0"

  val luceneDeps = Seq(
    "org.apache.lucene" % "lucene-core" % luceneVersion,
    "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion
  )

  val liftDeps = Seq(
    liftPackage %% "lift-util" % liftVersion exclude("joda-time", "joda-time"),
    liftPackage %% "lift-json-ext" % liftVersion exclude("joda-time", "joda-time"),
    liftPackage %% "lift-webkit" % liftVersion,
    liftPackage %% "lift-wizard" % liftVersion,
    liftPackage %% "lift-mongodb-record" % liftVersion exclude("org.mongodb", "mongo-java-driver"),
    liftPackage %% "lift-json-scalaz7" % liftVersion exclude("org.scalaz", "scalaz-core_2.9.1"),
    "net.liftmodules" %% "oauth_2.6" % "1.2-SNAPSHOT",
    liftPackage %% "lift-testkit" % liftVersion % "test",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "com.thoughtworks.paranamer" % "paranamer" % "2.8" //for json
  )

  val akkaDeps = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVer,
    "com.typesafe.akka" %% "akka-stream" % akkaVer,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVer,
    "com.typesafe.akka" %% "akka-testkit" % akkaVer % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVer % Test
  )

  val rogueDeps = Seq(
    "com.foursquare" %% "rogue-lift" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-field" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-index" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-core" % rogueVer intransitive()
  )

  val kotonohaRestDeps = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    "net.codingwell" %% "scala-guice" % "4.0.1",

    "org.scalaz" %% "scalaz-core" % "7.2.0",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "javax.transaction" % "jta" % "1.0.1B" % "provided",

    "org.mongodb" %% "casbah" % "2.8.2" exclude("org.specs2", "*"),

    "net.java.sen" % "lucene-gosen" % "6.0",

    "com.google.zxing" % "javase" % "3.2.1",
    "org.bouncycastle" % "bcprov-jdk15on" % "1.54",

    "com.j256.ormlite" % "ormlite-jdbc" % "4.42" % "test",
    "org.xerial" % "sqlite-jdbc" % "3.6.16" % "test"
  )

  import com.earldouglas.xwp.WebappPlugin.{autoImport => wapp}
  import com.typesafe.sbt.web.Import._

  lazy val kotonohaSettings =
    gitSettings ++
      binfo ++
      scalatest ++ Def.settings(
      name := "server",
      parallelExecution in Test := false,
      javacOptions ++= Seq("-encoding", "utf8"),
      scalacOptions ++= Seq("-unchecked", "-language:postfixOps"),

      jsDir := file("jslib"),
      scriptOutputDir := (target in com.earldouglas.xwp.WebappPlugin.autoImport.webappPrepare).value / "static",
      (sourceDirectory in compileJs) <<= sourceDirectory in Compile,
      postsMdFiles <<= baseDirectory { (base) =>
        val start = base / "doc" / "news"
        (start ** GlobFilter("*.md")).get.map { s =>
          s.relativeTo(start).get.toString
        }
      },
      (unmanagedResourceDirectories in Compile) += baseDirectory.value / "doc",
      unzipJars <<= (jsDir, scriptOutputDir) map {
        (jsd, so) =>
          (jsd * GlobFilter("*.jar")) flatMap { f => IO.unzip(f, so) } get
      },
      compile in compileJs := Analysis.Empty,
      resourceGenerators in compileJs := Nil,
      copyResources in Compile <<= (copyResources in Compile).dependsOn(unzipJars),
      WebKeys.stagingDirectory := (target in wapp.webappPrepare).value / "static",
      SassKeys.cssStyle := Maxified,
      SassKeys.generateSourceMaps := false,
      wapp.webappPrepare <<= wapp.webappPrepare.dependsOn(WebKeys.stage),
      JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
      resolvers += "eiennohito's repo" at "http://eiennohito.github.com/maven/",
      libraryDependencies ++= (liftDeps ++ akkaDeps ++ rogueDeps ++ luceneDeps ++ kotonohaRestDeps)
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
