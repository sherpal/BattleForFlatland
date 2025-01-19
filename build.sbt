import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import java.nio.charset.StandardCharsets
import scala.sys.process.Process

name := "Battle for Flatland"

version := "0.2"

ThisBuild / scalaVersion := "3.5.0"

Global / onLoad := {
  val scalaVersionValue = (frontend / scalaVersion).value
  val outputFile        = baseDirectory.value / "frontend" / "scala-metadata.js"
  IO.writeLines(
    outputFile,
    s"""
       |const scalaVersion = "$scalaVersionValue"
       |
       |exports.scalaMetadata = {
       |  scalaVersion: scalaVersion
       |}
       |""".stripMargin.split("\n").toList,
    StandardCharsets.UTF_8
  )

  println("""
  |    ____        __  __  __        ____              ________      __  __                __
  |   / __ )____ _/ /_/ /_/ /__     / __/___  _____   / ____/ /___ _/ /_/ /___ _____  ____/ /
  |  / __  / __ `/ __/ __/ / _ \   / /_/ __ \/ ___/  / /_  / / __ `/ __/ / __ `/ __ \/ __  / 
  | / /_/ / /_/ / /_/ /_/ /  __/  / __/ /_/ / /     / __/ / / /_/ / /_/ / /_/ / / / / /_/ /  
  |/_____/\__,_/\__/\__/_/\___/  /_/  \____/_/     /_/   /_/\__,_/\__/_/\__,_/_/ /_/\__,_/   
  |                                                                                         
  |                                                                                    
  |""".stripMargin)

  (Global / onLoad).value
}

val scalaCompilerOptions = List(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
//  "-unchecked",
//  "-Xlint",
//  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard"
  // "-Ywarn-dead-code"
)

ThisBuild / scalacOptions := scalaCompilerOptions

lazy val `shared` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    SharedSettings()
  )
  .jvmSettings(
    SharedSettings.jvmSettings
  )
  .jsSettings(
    SharedSettings.jsSettings
  )

lazy val `shared-backend` = project
  .in(file("./shared-backend"))
  .settings(
    BackendSettings(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(shared.jvm)

lazy val server = project
  .in(file("./server"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass       := Some("server.Server"),
    assembly / assemblyJarName := "app.jar"
  )
  .dependsOn(`shared-backend`)

def esModule = Def.settings(scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.ESModule)
})

lazy val `game-server-launcher` = project
  .in(file("./game-server-launcher"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass       := Some("server.Server"),
    assembly / assemblyJarName := "game-server-launcher.jar"
  )
  .dependsOn(`shared-backend`)

lazy val `game-server` = project
  .in(file("./game-server"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass       := Some("server.Server"),
    assembly / assemblyJarName := "game-server.jar",
    SharedSettings()
  )
  .dependsOn(`shared-backend`)

val indigoVersion = "0.17.0"

lazy val frontend = project
  .in(file("./frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= List(
      "com.raquo"       %%% "laminar"            % "17.0.0",
      "be.doeraene"     %%% "web-components-ui5" % "2.0.0",
      "io.indigoengine" %%% "indigo"             % indigoVersion,
      "io.indigoengine" %%% "indigo-extras"      % indigoVersion,
      "io.indigoengine" %%% "indigo-json-circe"  % indigoVersion
    ),
    esModule,
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(`shared`.js)

val buildFrontend = taskKey[Unit]("Build frontend")

buildFrontend := {
  /*
  To build the frontend, we do the following things:
  - fullLinkJS the frontend sub-module
  - run npm ci in the frontend directory (might not be required)
  - package the application with vite-js (output will be in the resources of the server sub-module)
   */
  (frontend / Compile / fullLinkJS).value
  val npmCiExit =
    Process(Utils.npm :: "ci" :: Nil, cwd = baseDirectory.value / "frontend").run().exitValue()
  if (npmCiExit > 0) {
    throw new IllegalStateException(s"npm ci failed. See above for reason")
  }

  val buildExit = Process(
    Utils.npm :: "run" :: "build" :: Nil,
    cwd = baseDirectory.value / "frontend"
  ).run().exitValue()
  if (buildExit > 0) {
    throw new IllegalStateException(s"Building frontend failed. See above for reason")
  }

  IO.copyDirectory(
    baseDirectory.value / "frontend" / "dist",
    baseDirectory.value / "server" / "src" / "main" / "resources" / "static"
  )
}

(server / assembly) := (server / assembly).dependsOn(buildFrontend).value

val packageApplication = taskKey[File]("Package the whole application into a fat jar")

packageApplication := {
  /*
  To package the whole application into a fat jar, we do the following things:
  - call sbt assembly to make the fat jar for us (config in the server sub-module settings)
  - we move it to the ./dist folder so that the Dockerfile can be independent of Scala versions and other details
   */
  val fatJar = (server / assembly).value
  val target = baseDirectory.value / "dist" / "app.jar"
  IO.copyFile(fatJar, target)
  target
}
