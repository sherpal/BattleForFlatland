package assets

import assets.ingame.gui.bars.{LiteStepBar, XeonBar}

import scala.language.implicitConversions
import scala.scalajs.js

trait Asset extends js.Object

object Asset {

  implicit def assetAsString(asset: Asset): String = asset.asInstanceOf[String]

  // Touching all assets so that there are loaded
  ScalaLogo
  XeonBar
  LiteStepBar

}
