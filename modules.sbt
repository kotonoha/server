import org.irundaia.sbt.sass._

lazy val kotonoha = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JettyPlugin, SbtWeb)
  .settings(Common.buildSettings)
  .settings(Kotonoha.kotonohaSettings, Pbuf.pbScala(), Pbuf.protoIncludes(eapi, `akane-knp`, `akane-dic`, model))
  .settings(jrebelSettings, libraryDependencies ++= Kotonoha.luceneDeps)
  .settings(
    pipelineStages in Assets += postcss,
    includeFilter in SassKeys.sassify := "main.scss"
  )
  .dependsOn(
    `akane-legacy`, knockoff, eapi, `grpc-streaming`, `akane-kytea`, `akane-jmdict-lucene`,
    model, `lift-tcjson`, `akane-akka`, `lift-macros` % Provided)

lazy val akane = (project in file("akane"))
  .settings(Common.buildSettings)

lazy val `akane-knp` = (project in file("akane/knp"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala())

lazy val `akane-dic` = (project in file("akane/dic"))
  .settings(Common.buildSettings, Pbuf.pbScala())

lazy val `akane-akka` = (project in file("akane/akka"))
  .settings(Common.buildSettings)

lazy val `akane-legacy` = (project in file("akane/legacy"))
  .settings(Common.buildSettings)

lazy val `akane-kytea` = (project in file("akane/kytea"))
  .settings(Common.buildSettings)

lazy val `akane-jmdict-lucene` = (project in file("akane/dic/jmdict-lucene"))
  .settings(Common.buildSettings)
  .dependsOn(`akane-dic`)

lazy val knockoff = (project in file("knockoff"))
  .settings(Common.buildSettings)

lazy val eapi = (project in file("examples-api"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala())
  .dependsOn(`grpc-streaming`)

lazy val `grpc-streaming` = (project in file("grpc-akka-stream"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala(), libraryDependencies ++= Kotonoha.akkaDeps)
  .setSbtFiles(file("../sbt-overrides/overrides.sbt"))

lazy val model = (project in file("model"))
    .settings(Common.buildSettings, Pbuf.pbScala())

lazy val `lift-macros` = (project in file("lift-macros"))
    .settings(Common.buildSettings)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
    )
    .dependsOn(`lift-tcjson`)

lazy val `lift-tcjson` = (project in file("lift-tcjson"))
    .settings(Common.buildSettings)
    .settings(
      libraryDependencies ++= Seq(
        Kotonoha.liftPackage %% "lift-json" % Kotonoha.liftVersion,
        Kotonoha.liftPackage %% "lift-common" % Kotonoha.liftVersion,
        Kotonoha.liftPackage %% "lift-record" % Kotonoha.liftVersion
      )
    )
