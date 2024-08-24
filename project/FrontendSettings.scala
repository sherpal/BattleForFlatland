import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.settings
import sbt.Keys.{baseDirectory, libraryDependencies, scalacOptions, version}
//import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object FrontendSettings {

  def apply(): Seq[Def.Setting[_]] = settings(
  )

}
