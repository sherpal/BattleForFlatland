val scalaJSVersion = "1.6.0"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.0")

//addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.18.0")

addSbtPlugin("com.heroku" % "sbt-heroku" % "2.1.3")

// resolvers += Resolver.bintrayRepo("oyvindberg", "converter")
// addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta13")
resolvers += Resolver.bintrayRepo("oyvindberg", "converter")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta29.1")

addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.9.1-PLAY2.8")
