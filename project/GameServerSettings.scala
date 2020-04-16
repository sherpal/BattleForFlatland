import sbt._
import sbt.Def.settings
import sbt.Keys.libraryDependencies

object GameServerSettings {

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.11",
      "com.typesafe.akka" %% "akka-stream" % "2.6.4",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.4",
      "com.github.scopt" %% "scopt" % "4.0.0-RC2"
    )
  )

}
