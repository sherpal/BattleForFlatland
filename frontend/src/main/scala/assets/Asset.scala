package assets

import scala.language.implicitConversions
import scala.scalajs.js

trait Asset extends js.Object

object Asset {

  implicit def assetAsString(asset: Asset): String = asset.asInstanceOf[String]

}
