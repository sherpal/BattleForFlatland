import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.settings
import sbt._
import sbt.Keys.libraryDependencies

object SharedSettings {

  val circeVersion = "0.13.0"
  val catsVersion  = "2.1.2"
  val zioVersion   = "1.0.0-RC18-2"

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %%% "core" % "2.0.6", // http requests
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "be.doeraene" %%% "url-dsl" % "0.1.4",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC5",
      "com.lihaoyi" %%% "scalatags" % "0.8.2"
    ) ++ Seq( // circe for json serialisation
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser",
      "io.circe" %%% "circe-shapes",
      "io.circe" %%% "circe-generic-extras"
    ).map(_ % circeVersion) ++ Seq(
      "org.typelevel" %%% "cats-effect" % "2.1.2",
      "org.typelevel" %%% "cats-core" % "2.1.1"
    ) ++ Seq(
      "dev.zio" %%% "zio-test" % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %%% "zio-test-magnolia" % zioVersion % "test" // optional
    )
  )

  def jvmSettings: Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.8.1"
    )
  )

  def jsSettings: Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      )
  )

}
