import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import java.nio.charset.StandardCharsets

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

  (Global / onLoad).value
}

val scalaCompilerOptions = List(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
//  "-unchecked",
  // "-Xfatal-warnings",
//  "-Xlint",
//  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard"
  //"-Ywarn-dead-code"
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
      // cask as server (other choices are zio-http, http4s, akka-http, play...)
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass       := Some("server.Server"),
    assembly / assemblyJarName := "app.jar"
  )
  .dependsOn(`shared-backend`)

def esModule = Def.settings(scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.ESModule)
})

lazy val frontend = project
  .in(file("./frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= List(
      "com.raquo"   %%% "laminar"            % "17.0.0",
      "be.doeraene" %%% "web-components-ui5" % "2.0.0-RC2"
    ),
    esModule,
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(`shared`.js)
