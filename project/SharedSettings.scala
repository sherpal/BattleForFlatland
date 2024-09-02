import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.settings
import sbt._
import sbt.Keys.libraryDependencies

object SharedSettings {

  val circeVersion = "0.14.9"
  val catsVersion  = "2.1.2"
  val zioVersion   = "2.1.7"

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "dev.zio"                      %%% "zio"             % zioVersion,
      "dev.zio"                      %%% "zio-streams"     % zioVersion,
      "be.doeraene"                  %%% "url-dsl"         % "0.6.2",
      "io.github.cquiroz"            %%% "scala-java-time" % "2.6.0",
      "com.lihaoyi"                  %%% "scalatags"       % "0.13.1",
      "com.softwaremill.magnolia1_3" %%% "magnolia"        % "1.3.7",
      "org.scala-lang"                 % "scala-reflect"   % "2.13.1" % Provided,
      "org.planet42"                 %%% "laika-core"      % "0.19.5",
      "io.suzaku"                    %%% "boopickle"       % "1.5.0",
      "org.scalacheck"               %%% "scalacheck"      % "1.18.0" % Test,
      "org.scalameta"                %%% "munit"           % "1.0.1"  % Test
    ) ++ Seq( // circe for json serialisation
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % circeVersion) ++ Seq(
      "dev.zio" %%% "zio-test"          % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt"      % zioVersion % "test",
      "dev.zio" %%% "zio-test-magnolia" % zioVersion % "test" // optional
    )
  )

  def jvmSettings: Seq[Def.Setting[_]] = settings(
  )

  def jsSettings: Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time-tzdb"      % "2.6.0",
      ("org.scala-js"     %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13)
    )
  )

}
