name := "kotonoha-model"

version := "0.1-SNAPSHOT"

crossPaths := false

libraryDependencies := Seq("joda-time" % "joda-time" % "1.6.2" % "compile")

libraryDependencies += "com.google.code.gson" % "gson" % "2.1" % "compile"

libraryDependencies += "com.j256.ormlite" % "ormlite-core" % "4.33"

libraryDependencies += "org.scribe" % "scribe" % "1.3.0"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))