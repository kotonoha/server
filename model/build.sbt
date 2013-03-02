name := "kotonoha-model"

version := "0.2-SNAPSHOT"

crossPaths := false

libraryDependencies := Seq(
  "joda-time" % "joda-time" % "2.1",
  "org.joda" % "joda-convert" % "1.2"
)

libraryDependencies += "com.google.code.gson" % "gson" % "2.2.2" % "compile"

libraryDependencies += "com.j256.ormlite" % "ormlite-core" % "4.43"

libraryDependencies += "org.scribe" % "scribe" % "1.3.3"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
