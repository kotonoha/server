addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.4.2")

resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.0.201606070830-r"

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin("me.lessis" % "coffeescripted-sbt" % "0.2.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
