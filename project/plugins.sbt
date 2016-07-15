addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "2.1.0")

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.0.201606070830-r",
  "com.github.os72" % "protoc-jar" % "3.0.0-b3"
)

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.0")
addSbtPlugin("io.teamscala.sbt" % "sbt-babel" % "1.0.5")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

val scalaPbVersion = "0.5.32"

addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % scalaPbVersion)

addSbtPlugin("fi.gekkio.sbtplugins" % "sbt-jrebel-plugin" % "0.10.0")
