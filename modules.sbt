lazy val kotonoha = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JettyPlugin, SbtWeb)
  .dependsOn(model, `akane-legacy`, knockoff, eapi, `grpc-streaming`)
  .settings(Common.buildSettings)
  .settings(Kotonoha.kotonohaSettings, Pbuf.pbScala(), Pbuf.protoIncludes(eapi, `akane-knp`, `akane-dic`))
  .settings(jrebelSettings)
  .dependsOn(model, `akane-legacy`, knockoff, eapi, `grpc-streaming`, jmdict)
  .enablePlugins(BuildInfoPlugin, JettyPlugin)
  .aggregate(jmdict)

lazy val akane = (project in file("akane"))
  .settings(Common.buildSettings)

lazy val `akane-knp` = (project in file("akane/knp"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala())

lazy val `akane-dic` = (project in file("akane/dic"))
  .settings(Common.buildSettings, Pbuf.pbScala())

lazy val `akane-legacy` = (project in file("akane/legacy"))
  .settings(Common.buildSettings)

lazy val model = (project in file("model"))
  .settings(Common.buildSettings)
  .settings(Kotonoha.modelSettings)

lazy val knockoff = (project in file("knockoff"))
  .settings(Common.buildSettings)

lazy val eapi = (project in file("examples-api"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala())
  .dependsOn(`grpc-streaming`)

lazy val `grpc-streaming` = (project in file("grpc-akka-stream"))
  .settings(Common.buildSettings)

lazy val jmdict = (project in file("jmdict"))
    .settings(Common.buildSettings, Kotonoha.scalatest)
    .settings(
      libraryDependencies ++= Kotonoha.luceneDeps,
      libraryDependencies ++= Seq(
        "com.github.ben-manes.caffeine" % "caffeine" % "2.3.1",
        "joda-time" % "joda-time" % "2.9.4"
      )
    ).dependsOn(`akane-dic`)
