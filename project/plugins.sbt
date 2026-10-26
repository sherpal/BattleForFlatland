val scalaJSVersion = "1.22.0"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("io.spray"           % "sbt-revolver"             % "0.10.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                  % "1.0.0")
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"             % "2.2.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.0.0")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"             % "0.14.9")

// To package the LÖVE client with LÖVE's builds (project/LovePackage.scala)
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.27.1"
