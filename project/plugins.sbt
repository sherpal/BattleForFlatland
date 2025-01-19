val scalaJSVersion = "1.16.0"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("io.spray"           % "sbt-revolver"             % "0.10.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                  % "1.0.0")
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"             % "2.2.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.0.0")
