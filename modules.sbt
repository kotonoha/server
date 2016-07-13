lazy val kotonoha = (project in file("."))
  .settings(Common.buildSettings)
  .settings(Kotonoha.kotonohaSettings)
  .dependsOn(model, `akane-legacy`, knockoff)

lazy val akane = (project in file("akane"))
  .settings(Common.buildSettings)

lazy val `akane-legacy` = (project in file("akane/legacy"))
    .settings(Common.buildSettings)

lazy val model = (project in file("model"))
  .settings(Common.buildSettings)
  .settings(Kotonoha.modelSettings)

lazy val knockoff = (project in file("knockoff"))
  .settings(Common.buildSettings)
