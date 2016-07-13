lazy val kotonoha = (project in file("."))
  .settings(Common.buildSettings)
  .settings(Kotonoha.kotonohaSettings, Pbuf.pbScala(), Pbuf.protoIncludes(eapi, `akane-knp`))
  .dependsOn(model, `akane-legacy`, knockoff, eapi, `grpc-streaming`)

lazy val akane = (project in file("akane"))
  .settings(Common.buildSettings)

lazy val `akane-knp` = (project in file("akane/knp"))
  .settings(Common.buildSettings)
  .settings(Pbuf.pbScala())

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

lazy val `grpc-streaming` = (project in file("grpc-akka-stream"))
  .settings(Common.buildSettings)
