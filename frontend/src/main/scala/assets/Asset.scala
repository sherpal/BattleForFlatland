package assets

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

trait Asset extends js.Object

object Asset {

  object ingame {
    object gui {
      object bars {
        @js.native @JSImport("resources/assets/in-game/gui/bars/LiteStep.png", JSImport.Default)
        object liteStepBar extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/bars/Minimalist.png", JSImport.Default)
        object minimalistBar extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/bars/Xeon.png", JSImport.Default)
        object xeonBar extends Asset

//        @js.native @JSImport("resources/assets/in-game/gui/bars/LiteStep.png", JSImport.Default)
//        val liteStepBar: Asset = js.native
//
//        @js.native @JSImport("resources/assets/in-game/gui/bars/Minimalist.png", JSImport.Default)
//        val minimalistBar: Asset = js.native
//
//        @js.native @JSImport("resources/assets/in-game/gui/bars/Xeon.png", JSImport.Default)
//        val xeonBar: Asset = js.native
      }
    }
  }

  implicit def assetAsString(asset: Asset): String = asset.asInstanceOf[String]

  // Touching all assets so that there are loaded
  ScalaLogo
  ingame.gui.bars.liteStepBar
  ingame.gui.bars.minimalistBar
  ingame.gui.bars.xeonBar

}
