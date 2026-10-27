import sbt.Keys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "Battle for Flatland"

version := "0.2"

ThisBuild / scalaVersion := "3.9.0"

Global / onLoad := {
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
  "-Werror",
  "-Wunused:linted",
  "-deprecation",
  "-unchecked",
  "-language:higherKinds",
  "-feature",
  "-language:implicitConversions"
)

ThisBuild / scalacOptions := scalaCompilerOptions

inThisBuild(
  List(
    semanticdbEnabled := true // enable SemanticDB
  )
)

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

/** Puts the fat jar directly in the target directory (e.g. `game-server/target/game-server.jar`)
  * instead of the default `target/scala-<version>/`, so that the paths used to launch it don't change
  * with the Scala version.
  */
def stableAssemblyOutputPath = Def.settings(
  assembly / assemblyOutputPath := target.value / (assembly / assemblyJarName).value
)

lazy val `game-server-launcher` = project
  .in(file("./game-server-launcher"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass       := Some("server.Server"),
    assembly / assemblyJarName := "game-server-launcher.jar",
    stableAssemblyOutputPath
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
    stableAssemblyOutputPath,
    SharedSettings()
  )
  .dependsOn(`shared-backend`)

/** The Scala.js IR -> Lua linker, which links `love-client` (a plain JVM project, run by sbt). */
lazy val `lua-linker` = project
  .in(file("./lua-linker"))
  .settings(
    libraryDependencies += "org.scala-js" %% "scalajs-linker" % scalaJSVersion
  )

/** The game's client, with LÖVE: Scala.js code linked to Lua by `lua-linker` (`love-client/luaLink`
  * writes the program in love-client/target/lua, `love-client/luaRun` runs it, and
  * `love-client/luaPackage` packages it with LÖVE for Windows, macOS and Linux, in
  * love-client/target/dist).
  */
lazy val `love-client` = project
  .in(file("./love-client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    LuaSettings(`lua-linker`),
    LuaSettings.luaPackageTitle     := "Battle for Flatland",
    // there is also `bffclient.lua.UdpSelfTest`, linked by tools/love-udp-test.sh
    Compile / mainClass             := Some("bffclient.Main"),
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(`shared`.js)

val packageApplication = taskKey[File]("Package the menu server into a fat jar (dist/app.jar)")

packageApplication := {
  /*
  To package the menu server (only its API: the client is the LÖVE one) into a fat jar:
  - call sbt assembly to make the fat jar for us (config in the server sub-module settings)
  - we move it to the ./dist folder so that the Dockerfile can be independent of Scala versions and other details
   */
  val fatJar = (server / assembly).value
  val target = baseDirectory.value / "dist" / "app.jar"
  IO.copyFile(fatJar, target)
  target
}
