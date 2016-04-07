import sbt._
import KotonohaBuild._

organization := "ws.kotonoha"

name := "server"

version := "0.2-SNAPSHOT"

moduleName := "kotonoha"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

resolvers += "java.net" at "http://download.java.net/maven/2"

libraryDependencies ++=
					Seq("org.scalaz" %% "scalaz-core" % "7.2.0",
						"com.jsuereth" %% "scala-arm" % "1.4",
            "javax.transaction" % "jta" % "1.0.1B" % "provided"
					)

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"

libraryDependencies +=  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

resolvers += "eiennohito's repo" at "http://eiennohito.github.com/maven/"

libraryDependencies += "fuku.eb4j" % "eb4j-tools" % "1.0.5"

libraryDependencies ++= {
  val liftVersion = "2.6.3" // Put the current/latest lift version here
  Seq(
    "net.liftweb" %% "lift-util" % liftVersion % "compile->default" exclude("joda-time", "joda-time"),
    "net.liftweb" %% "lift-json-ext" % liftVersion % "compile->default" exclude("joda-time", "joda-time"),
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mongodb-record" % liftVersion exclude("org.mongodb", "mongo-java-driver"),
    "net.liftweb" %% "lift-json-scalaz7" % liftVersion exclude("org.scalaz", "scalaz-core_2.9.1"),
    "net.liftmodules" %% "oauth_2.6" % "1.2-SNAPSHOT",
    "net.liftweb" %% "lift-testkit" % liftVersion % "test",
	  "javax.servlet" % "servlet-api" % "2.5" % "provided")
}

libraryDependencies ++= {
  Seq(
    "org.mongodb" %% "casbah" % "2.8.2" exclude("org.specs2", "*")
  )
}

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= {
  val akkaVer = "2.4.2"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVer,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVer,
    "com.typesafe.akka" %% "akka-testkit" % akkaVer % "test"
  )
}

libraryDependencies +=  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

libraryDependencies ++=  {
  val rogueVer = "2.5.1"
  Seq(
    "com.foursquare" %% "rogue-lift" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-field" % "2.5.0" intransitive(),
    "com.foursquare" %% "rogue-index" % rogueVer intransitive(),
    "com.foursquare" %% "rogue-core" % rogueVer intransitive()
  )
}

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13" % "compile"

libraryDependencies += "com.j256.ormlite" % "ormlite-jdbc" % "4.42" % "test"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.6.16" % "test"

libraryDependencies += "net.java.sen" % "lucene-gosen" % "2.1" exclude ("org.slf4j", "slf4j-jdk14")

libraryDependencies += "com.google.zxing" % "javase" % "2.0"

libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.47"

javacOptions ++= Seq("-encoding", "utf8")

scalacOptions ++= Seq("-unchecked", "-language:postfixOps")

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

seq(buildInfoSettings: _*)

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, gitId, gitDate)

buildInfoPackage := "ws.kotonoha.server.util"

buildInfoObject  := "BuildInfo"

scanInterval in Compile := 0

net.virtualvoid.sbt.graph.Plugin.graphSettings

parallelExecution in Test := false
