import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.settings
import sbt._
import sbt.Keys.libraryDependencies

object SharedSettings {

  val circeVersion = "0.13.0"
  val catsVersion  = "2.1.2"
  val zioVersion   = "1.0.0"

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %%% "core" % "2.1.2", // http requests
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "be.doeraene" %%% "url-dsl" % "0.2.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC5",
      "com.lihaoyi" %%% "scalatags" % "0.9.1",
      "com.propensive" %%% "magnolia" % "0.16.0",
      "org.scala-lang" % "scala-reflect" % "2.13.1" % Provided,
      "org.planet42" %%% "laika-core" % "0.15.0",
      "io.suzaku" %%% "boopickle" % "1.3.3",
      "org.scalacheck" %%% "scalacheck" % "1.14.3" % Test,
      "org.scalameta" %%% "munit" % "0.7.20" % Test
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
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0"
    )
  )

}
