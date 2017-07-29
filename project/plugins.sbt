addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "2.2.0")

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.0.201606070830-r",
  "com.github.os72" % "protoc-jar" % "3.1.0.1"
)

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.6")
addSbtPlugin("io.teamscala.sbt" % "sbt-babel" % "1.0.5")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

val scalaPbVersion = "0.6.1"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.11")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % scalaPbVersion

addSbtPlugin("fi.gekkio.sbtplugins" % "sbt-jrebel-plugin" % "0.10.0")

resolvers += Resolver.typesafeRepo("releases")

lazy val root = project.in(file(".")).dependsOn(sbtPostcss)
lazy val sbtPostcss = uri("git://github.com/kotonoha/sbt-postcss")
