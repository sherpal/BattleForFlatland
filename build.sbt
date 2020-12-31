import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "Battle for Flatland"

version := "0.2"

scalaVersion in ThisBuild := "2.13.4"

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
    scalaVersion := "2.13.4",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
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
    scalaVersion := "2.13.4",
    BackendSettings(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(shared.jvm)

/** Backend server uses Play framework */
lazy val `backend` = (project in file("./backend"))
  .enablePlugins(PlayScala)
  //.enablePlugins(SwaggerPlugin)
  .settings(
    scalaVersion := "2.13.4",
    BackendSettings.playSpecifics(),
    BackendSettings.testsDeps(),
    BackendSettings.herokuSettings(),
    swaggerDomainNameSpaces := Seq("models"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies += guice // dependency injection
  )
  .dependsOn(`shared-backend`)

lazy val `frontend` = (project in file("./frontend"))
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .disablePlugins(HerokuPlugin)
  //.enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    FrontendSettings(),
    scalaVersion := "2.13.4",
//    useYarn := true,
    stUseScalaJsDom := false,
    stIgnore := List(
      "@pixi/constants", 
      "@pixi/core", 
      "@pixi/math", 
      "@pixi/settings", 
      "@pixi/utils", 
      "tailwindcss"
    ),
    externalNpm := {
      scala.sys.process.Process("npm", baseDirectory.value).!
      baseDirectory.value
    },
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(shared.js)

lazy val `game-server` = project
  .in(file("./game-server"))
  .settings(
    scalaVersion := "2.13.4",
    GameServerSettings()
  )
  .disablePlugins(HerokuPlugin)
  .dependsOn(`shared-backend`)

lazy val bundlerSettings: Project => Project =
  _.settings(
    // Compile / fastOptJS / webpackExtraArgs += "--mode=development",
    // Compile / fullOptJS / webpackExtraArgs += "--mode=production",
    // Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
    // Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production"
  )

val nodeProject: Project => Project =
  _.settings(
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv,
    // es5 doesn't include DOM, which we don't have access to in node
    stStdlib := List("es5"),
    stUseScalaJsDom := false,
    // Compile / npmDependencies ++= Seq(
    //   "@types/node" -> "13.5.0"
    // )
  )

lazy val `game-server-launcher` = project
  .in(file("./game-server-launcher"))
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .configure(bundlerSettings, nodeProject)
  .settings(
    externalNpm := {
      scala.sys.process.Process("npm", baseDirectory.value).!
      baseDirectory.value
    },
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .disablePlugins(HerokuPlugin)

addCommandAlias("dev", ";frontend/fastOptJS::startWebpackDevServer;~frontend/fastOptJS")

/**
  * This command builds the frontend inside the backend public directory. This should only be used in production.
  */
addCommandAlias("build", "frontend/fullOptJS::webpack")

addCommandAlias("compileShared", ";sharedJS/compile;sharedJVM/compile")

stage := {
//  val webpackValue = (frontend / Compile / fullOptJS / webpack).value
//  println(s"Webpack value is $webpackValue")

  (stage in backend).value
}

// sbt clean stage backend/deploy
