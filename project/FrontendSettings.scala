import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.settings
import sbt.Keys.{baseDirectory, libraryDependencies, scalacOptions, version}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object FrontendSettings {

  def apply(): Seq[Def.Setting[_]] = settings(
    // Some npm dependencies
    npmDependencies in Compile ++= Seq(
      "pixi.js" -> "5.2.1",
      "@pixi/core" -> "5.2.1",
      "pixi-filters" -> "3.1.0"
    ),
//    npmDevDependencies in Compile += "file-loader" -> "6.0.0",
//    npmDevDependencies in Compile += "style-loader" -> "1.1.3",
//    npmDevDependencies in Compile += "css-loader" -> "3.4.2",
//    npmDevDependencies in Compile += "html-webpack-plugin" -> "3.2.0",
//    npmDevDependencies in Compile += "copy-webpack-plugin" -> "5.1.1",
//    npmDevDependencies in Compile += "webpack-merge" -> "4.2.2",
    npmDevDependencies in Compile += "file-loader" -> "3.0.1",
    npmDevDependencies in Compile += "style-loader" -> "0.23.1",
    npmDevDependencies in Compile += "css-loader" -> "2.1.1",
    npmDevDependencies in Compile += "html-webpack-plugin" -> "3.2.0",
    npmDevDependencies in Compile += "copy-webpack-plugin" -> "5.0.2",
    npmDevDependencies in Compile += "webpack-merge" -> "4.2.1",
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    version in webpack := "4.29.6",
    version in startWebpackDevServer := "3.4.1",
    webpackResources := baseDirectory.value / "webpack" * "*",
    webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack" / "webpack-fastopt.config.js"),
    webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack" / "webpack-opt.config.js"),
    webpackConfigFile in Test := Some(baseDirectory.value / "webpack" / "webpack-core.config.js"),
    webpackDevServerExtraArgs in fastOptJS := Seq("--inline", "--hot"),
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(),
    requireJsDomEnv in Test := true,
    // laminar
    libraryDependencies += "com.raquo" %%% "laminar" % "0.7.2"
  )

}
