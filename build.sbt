import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "Battle for Flatland"

version := "0.2"

scalaVersion in ThisBuild := "2.13.1"

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

scalacOptions in ThisBuild := scalaCompilerOptions

lazy val `shared` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .disablePlugins(HerokuPlugin) // no need of Heroku for shared project
  .settings(
    SharedSettings(),
    scalaVersion := "2.13.1",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .jvmSettings(
    SharedSettings.jvmSettings
  )
  .jsSettings(
    SharedSettings.jsSettings
  )

lazy val `shared-backend` = project
  .in(file("./shared-backend"))
  .disablePlugins(HerokuPlugin)
  .settings(
    scalaVersion := "2.13.1",
    BackendSettings(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(shared.jvm)

/** Backend server uses Play framework */
lazy val `backend` = (project in file("./backend"))
  .enablePlugins(PlayScala)
  //.enablePlugins(SwaggerPlugin)
  .settings(
    scalaVersion := "2.13.1",
    BackendSettings.playSpecifics(),
    BackendSettings.testsDeps(),
    BackendSettings.herokuSettings(),
    swaggerDomainNameSpaces := Seq("models"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies += guice // dependency injection
  )
  .dependsOn(`shared-backend`)

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

lazy val `game-server` = project
  .in(file("./game-server"))
  .settings(
    scalaVersion := "2.13.1",
    GameServerSettings()
  )
  .disablePlugins(HerokuPlugin)
  .dependsOn(`shared-backend`)

lazy val `game-server-launcher` = project.in(file("./game-server-launcher"))
  .settings(
    libraryDependencies ++= List(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-http" % "10.1.12",
      "com.lihaoyi" %% "os-lib" % "0.7.0"
    )
  )
  .disablePlugins(HerokuPlugin)

addCommandAlias("dev", ";frontend/fastOptJS::startWebpackDevServer;~frontend/fastOptJS")

/**
  * This command builds the frontend inside the backend public directory, using fastOptJS for shorter cycle
  * if you need to develop using this.
  */
addCommandAlias("fastBuild", "frontend/fastOptJS::webpack")

/**
  * This command builds the frontend inside the backend public directory. This should only be used in production.
  */
addCommandAlias("build", "frontend/fullOptJS::webpack")

addCommandAlias("compileShared", ";sharedJS/compile;sharedJVM/compile")

stage := {
  val webpackValue = (frontend / Compile / fullOptJS / webpack).value
  println(s"Webpack value is $webpackValue")
  (stage in backend).value
}

// sbt clean stage backend/deploy
