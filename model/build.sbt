name := "kotonoha-model"

version := "0.1-SNAPSHOT"

crossPaths := false

libraryDependencies += "joda-time" % "joda-time" % "1.6.2" % "compile"

libraryDependencies += "com.google.code.gson" % "gson" % "2.1" % "compile"

libraryDependencies += "com.j256.ormlite" % "ormlite-core" % "4.33"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))