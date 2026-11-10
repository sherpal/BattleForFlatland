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
    BackendSettings()
  )
  .dependsOn(shared.jvm)

lazy val server = project
  .in(file("./server"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi"   %% "cask"      % "0.9.1",
      "com.lihaoyi"   %% "scalatags" % "0.13.1",
      "org.scalameta" %% "munit"     % "1.0.1" % Test
    ),
    assembly / mainClass := Some("server.Server"),
    assembly / assemblyJarName := "app.jar"
  )
  .dependsOn(`shared-backend`)

/** Puts the fat jar directly in the target directory (e.g. `game-server/target/game-server.jar`)
  * instead of the default `target/scala-<version>/`, so that the paths used to launch it don't
  * change with the Scala version.
  */
def stableAssemblyOutputPath = Def.settings(
  assembly / assemblyOutputPath := target.value / (assembly / assemblyJarName).value
)

val gameServerRuntimes = taskKey[Seq[File]](
  "Make the Java runtimes that the players run the game server with (in game-server/target/runtimes)"
)

lazy val `game-server` = project
  .in(file("./game-server"))
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "cask" % "0.9.1"
    ),
    assembly / mainClass := Some("server.Server"),
    assembly / assemblyJarName := "game-server.jar",
    stableAssemblyOutputPath,
    // the JDKs (one per platform) are downloaded once, in the root's target directory
    gameServerRuntimes := JavaRuntimes(
      assembly.value,
      (LocalRootProject / target).value / "jdks",
      target.value / "runtimes",
      streams.value.log
    ),
    SharedSettings()
  )
  .dependsOn(`shared-backend`)

/** The Scala.js IR -> Lua linker, which links `love-client` (a plain JVM project, run by sbt). */
lazy val `lua-linker` = project
  .in(file("./lua-linker"))
  .settings(
    libraryDependencies += "org.scala-js" %% "scalajs-linker" % scalaJSVersion
  )

val packagedMenuServer = settingKey[String](
  "The menu server of the packaged clients, which do not ask for it: `host`, `host:port`, possibly with http:// (otherwise HTTPS). BFF_MENU_SERVER's, or battle-for-flatland.eu"
)

/** The game's client, with LÖVE: Scala.js code linked to Lua by `lua-linker` (`love-client/luaLink`
  * writes the program in love-client/target/lua, `love-client/luaRun` runs it, and
  * `love-client/luaPackage` packages it with LÖVE for Windows, macOS and Linux, in
  * love-client/target/dist, linked in prod mode).
  */
lazy val `love-client` = project
  .in(file("./love-client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    LuaSettings(`lua-linker`),
    LuaSettings.luaPackageTitle := "Battle for Flatland",
    packagedMenuServer := sys.env.get("BFF_MENU_SERVER").filter(_.nonEmpty).getOrElse("battle-for-flatland.eu"),
    // read by `bffclient.lua.BuildConfig`: in dev, the player types the menu server's address (it
    // runs locally, in plain HTTP)
    LuaSettings.luaConfig := Map("menuServerScheme" -> "http"),
    // the packages connect to packagedMenuServer, in HTTPS (behind Caddy, see deploy.md) unless it
    // says http://...
    LuaSettings.luaPackage / LuaSettings.luaConfig :=
      Map("menuServer" -> packagedMenuServer.value, "menuServerScheme" -> "https"),
    // there is also `bffclient.lua.UdpSelfTest`, linked by tools/love-udp-test.sh
    Compile / mainClass := Some("bffclient.Main"),
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

val deploy = inputKey[Unit](
  "Build everything and deploy the menu server on a VM (see deploy.md): deploy <user@host> [<platform>] [--key <file>] [--site <IP or domain>]"
)

deploy := {
  val args = Def.spaceDelimited(Deploy.Args.usage).parsed
  // everything that the Docker image copies
  val menuServerJar   = packageApplication.value
  val gameServerJar   = (`game-server` / assembly).value
  val runtimes        = (`game-server` / gameServerRuntimes).value
  val clientPackages  = (`love-client` / LuaSettings.luaPackage).value
  Deploy(baseDirectory.value, target.value / "deploy", Deploy.Args.parse(args), streams.value.log)
}
