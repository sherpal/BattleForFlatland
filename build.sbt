import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "Battle for Flatland"

version := "0.1"

scalaVersion := "2.13.1"

val scalaCompilerOptions = List(
  "-deprecation",
  "-feature"
//  "-unchecked",
  // "-Xfatal-warnings",
//  "-Xlint",
//  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard"
  //"-Ywarn-dead-code"
)

lazy val `shared` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .disablePlugins(HerokuPlugin) // no need of Heroku for shared project
  .settings(
    SharedSettings(),
    scalaVersion := "2.13.1",
    scalacOptions := scalaCompilerOptions
  )
  .jvmSettings(
    SharedSettings.jvmSettings
  )
  .jsSettings(
    SharedSettings.jsSettings
  )

/** Backend server uses Play framework */
lazy val `backend` = (project in file("./backend"))
  .enablePlugins(PlayScala)
  //.enablePlugins(SwaggerPlugin)
  .settings(
    scalaVersion := "2.13.1",
    BackendSettings(),
    BackendSettings.herokuSettings(),
    swaggerDomainNameSpaces := Seq("models"),
    libraryDependencies += guice // dependency injection
  )
  .dependsOn(shared.jvm)

lazy val `frontend` = (project in file("./frontend"))
  .enablePlugins(ScalablyTypedConverterPlugin)
  .disablePlugins(HerokuPlugin)
  //.enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    FrontendSettings(),
    scalaVersion := "2.13.1",
    useYarn := true,
    stUseScalaJsDom := false
  )
  .dependsOn(shared.js)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

addCommandAlias("dev", ";frontend/fastOptJS::startWebpackDevServer;~frontend/fastOptJS")

addCommandAlias("build", "frontend/fullOptJS::webpack")

stage := {
  val webpackValue = (frontend / Compile / fullOptJS / webpack).value
  println(s"Webpack value is $webpackValue")
  (stage in backend).value
}

// sbt clean stage backend/deploy
