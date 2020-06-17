import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalablytyped.converter.Flavour
import org.scalablytyped.converter.plugin.ScalablyTypedPluginBase.autoImport.stFlavour
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
      "@pixi/math" -> "5.2.1",
      "@pixi/utils" -> "5.2.1",
      "@pixi/settings" -> "5.2.1",
      "@pixi/constants" -> "5.2.1",
      "pixi-filters" -> "3.1.0",
      "@popperjs/core" -> "2.2.0",
      "marked" -> "0.8.2",
      "@types/marked" -> "0.7.3",
      "tailwindcss" -> "1.4.0",
      "@types/react-color" -> "3.0.2",
      "react-color" -> "2.18.1",
      "@types/react" -> "16.9.35",
      "react" -> "16.13.1",
      "@types/react-dom" -> "16.9.8",
      "react-dom" -> "16.13.1"
    ),
    npmDevDependencies in Compile ++= Seq(
      "file-loader" -> "3.0.1",
      "raw-loader" -> "4.0.1",
      "style-loader" -> "0.23.1",
      "css-loader" -> "2.1.1",
      "html-webpack-plugin" -> "3.2.0",
      "copy-webpack-plugin" -> "5.0.2",
      "webpack-merge" -> "4.2.1",
      "purgecss-laminar-webpack-plugin" -> "0.1.4",
      "postcss-import" -> "12.0.1",
      "postcss-loader" -> "3.0.0",
      "postcss-nested" -> "4.2.1",
      "node-sass" -> "4.13.1",
      "sass-loader" -> "8.0.2",
      "scalajs-friendly-source-map-loader" -> "0.1.4",
      "stylelint" -> "13.2.1",
      "stylelint-config-standard" -> "20.0.0",
      "stylelint-config-recommended" -> "3.0.0",
      "extract-css-chunks-webpack-plugin" -> "4.7.4"
    ),
    version in webpack := "4.40.2",
    version in startWebpackDevServer := "3.4.1",
    webpackResources := baseDirectory.value / "webpack" * "*",
    webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack" / "webpack-fastopt.config.js"),
    webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack" / "webpack-opt.config.js"),
    fullOptJS / scalaJSLinkerConfig ~= { _.withClosureCompiler(false) },
    webpackConfigFile in Test := Some(baseDirectory.value / "webpack" / "webpack-core.config.js"),
    webpackDevServerExtraArgs in fastOptJS := Seq("--inline", "--hot"),
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(),
    requireJsDomEnv in Test := true,
    // laminar
    libraryDependencies += "com.raquo" %%% "laminar" % "0.9.0",
    libraryDependencies += "me.shadaj" %%% "slinky-core" % "0.6.5", // core React functionality, no React DOM
    libraryDependencies += "me.shadaj" %%% "slinky-web" % "0.6.5", // React DOM, HTML and SVG tags
    libraryDependencies += "me.shadaj" %%% "slinky-native" % "0.6.5", // React Native components
    scalacOptions += "-Ymacro-annotations",
    stFlavour := Flavour.Slinky,
    scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(false)) }
  )

}
