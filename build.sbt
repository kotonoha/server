import sbt._

seq(webSettings :_*)

organization := "org.eiennohito"

name := "kotonoha-server"

version := "0.1-SNAPSHOT"

moduleName := "kotonoha"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies ++=
					Seq("org.scalaz" %% "scalaz-core" % "6.0.3",
						"com.github.jsuereth.scala-arm" %% "scala-arm" % "1.0"
					)

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-core" % "0.2.0"

libraryDependencies +=  "com.github.scala-incubator.io" %% "scala-io-file" % "0.2.0"

libraryDependencies ++= {
  val liftVersion = "2.4" // Put the current/latest lift version here
  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mongodb-record" % liftVersion,
    "net.liftweb" %% "lift-json-scalaz" % liftVersion,
    "net.liftweb" %% "lift-oauth" % liftVersion,
    "net.liftweb" %% "lift-testkit" % liftVersion % "test",
	  "javax.servlet" % "servlet-api" % "2.5" % "provided->default")
}

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= {
  val akkaVer = "2.0"
  Seq(
    "com.typesafe.akka" % "akka-actor" % akkaVer,
    "com.typesafe.akka" % "akka-slf4j" % akkaVer,
    "com.typesafe.akka" % "akka-testkit" % akkaVer % "test"
  )
}

libraryDependencies +=  "org.eclipse.jetty" % "jetty-webapp" % "8.0.4.v20111024" % "container"

libraryDependencies += "com.foursquare" %% "rogue" % "1.1.1" intransitive()

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.0" % "compile"

libraryDependencies += "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7"

libraryDependencies += "com.j256.ormlite" % "ormlite-jdbc" % "4.33" % "test"

resolvers += "some weird japanese repo" at "http://dev.mwsoft.jp/repo/"

libraryDependencies += "net.java.sen" % "lucene-gosen" % "1.1.1" exclude ("org.slf4j", "slf4j-jdk14")

libraryDependencies += "com.h2database" % "h2" % "1.3.163" % "test"

libraryDependencies += "com.google.zxing" % "javase" % "2.0"

javacOptions ++= Seq("-encoding", "utf8")

scalacOptions ++= Seq("-unchecked")

seq(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))