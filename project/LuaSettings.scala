import sbt._
import sbt.Keys._

/** Settings of the projects compiled to Lua (to run with LÖVE) by `lua-linker`, which links their
  * Scala.js IR to a Lua program instead of JavaScript.
  */
object LuaSettings {

  lazy val luaOutput = settingKey[File]("Directory where the Lua program is written")
  lazy val luaLink   = taskKey[File]("Link the program to Lua (main.lua), with its Lua modules")
  lazy val luaRun    = taskKey[Unit]("Link the program to Lua and run it with LÖVE")
  lazy val luaPackage =
    taskKey[Seq[File]]("Package the game with LÖVE for Windows, macOS and Linux (in target/dist)")
  lazy val luaPackageTitle =
    settingKey[String]("The packaged game's name (in camel case, the packages' file names)")
  lazy val luaProbe  =
    taskKey[Unit]("Print statistics about the linked IR (set DUMP_CLASS=a.B,c.D to dump classes)")

  /** @param linker
    *   the `lua-linker` project
    */
  def apply(linker: Project): Seq[Def.Setting[_]] = Seq(
    luaOutput := target.value / "lua",
    luaLink   := Def.taskDyn {
      val cp      = classpath.value
      val out     = luaOutput.value
      val mainCls = (Compile / mainClass).value.getOrElse(sys.error("No main class"))
      Def.task {
        (linker / Compile / runMain)
          .toTask(s" scalalove.linker.Main $mainCls ${out.getAbsolutePath} $cp")
          .value
        // Lua modules of the project (src/main/lua) are copied next to main.lua
        val luaSources = (Compile / sourceDirectory).value / "lua"
        if (luaSources.exists()) IO.copyDirectory(luaSources, out, overwrite = true)
        out
      }
    }.value,
    luaProbe := Def.taskDyn {
      val mainCls = (Compile / mainClass).value.getOrElse(sys.error("No main class"))
      (linker / Compile / runMain).toTask(s" scalalove.linker.Probe $mainCls ${classpath.value}")
    }.value,
    luaRun := {
      val dir = luaLink.value
      scala.sys.process.Process(Seq("love", dir.getAbsolutePath)).!
    },
    luaPackageTitle := name.value,
    // LÖVE's builds are downloaded once, in the root's target directory
    luaPackage := LovePackage(
      luaLink.value,
      luaPackageTitle.value,
      (LocalRootProject / target).value / s"love-${LovePackage.loveVersion}",
      target.value / "dist",
      streams.value.log
    )
  )

  private def classpath = Def.task {
    (Compile / fullClasspath).value.map(_.data.getAbsolutePath).mkString(java.io.File.pathSeparator)
  }
}
