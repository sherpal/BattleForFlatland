package assets

import gamelogic.abilities.Ability

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

trait Asset extends js.Object

object Asset {

  object ingame {
    object gui {
      object abilities {
        @js.native @JSImport("resources/assets/in-game/gui/abilities/ability-overlay.png", JSImport.Default)
        object abilityOverlay extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/abilities/hexagon-flash-heal.png", JSImport.Default)
        object hexagonFlashHeal extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/abilities/hexagon-hot.png", JSImport.Default)
        object hexagonHot extends Asset

      }

      object bars {
        @js.native @JSImport("resources/assets/in-game/gui/bars/LiteStep.png", JSImport.Default)
        object liteStepBar extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/bars/Minimalist.png", JSImport.Default)
        object minimalistBar extends Asset

        @js.native @JSImport("resources/assets/in-game/gui/bars/Xeon.png", JSImport.Default)
        object xeonBar extends Asset
      }
    }
  }

  implicit def assetAsString(asset: Asset): String = asset.asInstanceOf[String]

  // Touching all assets so that there are loaded
  ScalaLogo
  ingame.gui.bars.liteStepBar
  ingame.gui.bars.minimalistBar
  ingame.gui.bars.xeonBar
  ingame.gui.abilities.abilityOverlay
  ingame.gui.abilities.hexagonFlashHeal
  ingame.gui.abilities.hexagonHot

  final val abilityAssetMap: Map[Ability.AbilityId, Asset] = Map(
    Ability.hexagonHexagonHotId -> ingame.gui.abilities.hexagonHot,
    Ability.hexagonFlashHealId -> ingame.gui.abilities.hexagonFlashHeal
  )

}
