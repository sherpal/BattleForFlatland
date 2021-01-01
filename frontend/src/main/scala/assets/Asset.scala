package assets

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.gameextras.GameMarker

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try

sealed trait Asset {
  val name: String

  override final def equals(obj: Any): Boolean = obj match {
    case that: Asset => this.name == that.name
    case _           => false
  }

  override final def hashCode(): Int = name.hashCode()
}

object Asset {

  def apply(str: String): Asset = new Asset {
    val name: String = str
  }

  final class AssetNotProperlyDefined(asset: Asset)
      extends Exception(
        s"The asset `${asset.toString}` does not seem to work. Did you forget to restart the Dev server? Or Perhaps" +
          s"you forgot to add the asset in resource path? A typo in the filepath could also cause that."
      )

  object ingame {
    object gui {
      object abilities {
        // @js.native @JSImport("assets/in-game/gui/abilities/ability-overlay.png", JSImport.Default)
        // object abilityOverlay extends Asset
        val abilityOverlay = Asset("assets/in-game/gui/abilities/ability-overlay.png")

        // @js.native @JSImport("assets/in-game/gui/abilities/hexagon-flash-heal.png", JSImport.Default)
        // object hexagonFlashHeal extends Asset
        val hexagonFlashHeal = Asset("assets/in-game/gui/abilities/hexagon-flash-heal.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/hexagon-hot.png", JSImport.Default)
        // object hexagonHot extends Asset
        val hexagonHot = Asset("assets/in-game/gui/abilities/hexagon-hot.png")

        // @js.native @JSImport("assets/in-game/gui/abilities/square-hammer-hit.png", JSImport.Default)
        // object squareHammerHit extends Asset
        val squareHammerHit = Asset("assets/in-game/gui/abilities/square-hammer-hit.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/square-taunt.png", JSImport.Default)
        // object squareTaunt extends Asset
        val squareTaunt = Asset("assets/in-game/gui/abilities/square-taunt.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/square-enrage.png", JSImport.Default)
        // object squareEnrage extends Asset
        val squareEnrage = Asset("assets/in-game/gui/abilities/square-enrage.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/square-cleave.png", JSImport.Default)
        // object squareCleave extends Asset
        val squareCleave = Asset("assets/in-game/gui/abilities/square-cleave.png")

        // @js.native @JSImport("assets/in-game/gui/abilities/triangle-direct-hit.png", JSImport.Default)
        // object triangleDirectHit extends Asset
        val triangleDirectHit = Asset("assets/in-game/gui/abilities/triangle-direct-hit.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/triangle-upgrade-direct-hit.png", JSImport.Default)
        // object triangleUpgradeDirectHit extends Asset
        val triangleUpgradeDirectHit = Asset("assets/in-game/gui/abilities/triangle-upgrade-direct-hit.png")
        val triangleStun             = Asset("assets/in-game/gui/abilities/triangle-stun.png")

        // @js.native @JSImport("assets/in-game/gui/abilities/pentagon-bullet.png", JSImport.Default)
        // object pentagonBullet extends Asset
        val pentagonBullet = Asset("assets/in-game/gui/abilities/pentagon-bullet.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/create-pentagon-zone.png", JSImport.Default)
        // object pentagonZone extends Asset
        val pentagonZone = Asset("assets/in-game/gui/abilities/create-pentagon-zone.png")
        // @js.native @JSImport("assets/in-game/gui/abilities/pentagon-dispel.png", JSImport.Default)
        // object pentagonDispel extends Asset
        val pentagonDispel = Asset("assets/in-game/gui/abilities/pentagon-dispel.png")

        // @js.native @JSImport("assets/in-game/gui/abilities/boss101-big-dot.png", JSImport.Default)
        // object boss101BigDot extends Asset
        val boss101BigDot = Asset("assets/in-game/gui/abilities/boss101-big-dot.png")

      }

      object bars {
        // @js.native @JSImport("assets/in-game/gui/bars/LiteStep_wenakari.png", JSImport.Default)
        // object liteStepBar extends Asset
        val liteStepBar = Asset("assets/in-game/gui/bars/LiteStep_wenakari.png")

        // @js.native @JSImport("assets/in-game/gui/bars/Minimalist.png", JSImport.Default)
        // object minimalistBar extends Asset
        val minimalistBar = Asset("assets/in-game/gui/bars/Minimalist.png")

        // @js.native @JSImport("assets/in-game/gui/bars/Xeon.png", JSImport.Default)
        // object xeonBar extends Asset
        val xeonBar = Asset("assets/in-game/gui/bars/Xeon.png")
      }

      object boss {
        object dawnOfTime {
          object boss102 {
            // @js.native @JSImport(
            //   "assets/in-game/gui/boss/dawn-of-time/boss102/living-damage-zone.png",
            //   JSImport.Default
            // )
            // object livingDamageZone extends Asset
            val livingDamageZone = Asset("assets/in-game/gui/boss/dawn-of-time/boss102/living-damage-zone.png")
          }
          object boss103 {
            // @js.native @JSImport(
            //   "assets/in-game/gui/boss/dawn-of-time/boss103/boss103-punished.png",
            //   JSImport.Default
            // )
            // object punished extends Asset
            val punished = Asset("assets/in-game/gui/boss/dawn-of-time/boss103/boss103-punished.png")
            // @js.native @JSImport(
            //   "assets/in-game/gui/boss/dawn-of-time/boss103/boss103-purified.png",
            //   JSImport.Default
            // )
            // object purified extends Asset
            val purified = Asset("assets/in-game/gui/boss/dawn-of-time/boss103/boss103-purified.png")
            // @js.native @JSImport(
            //   "assets/in-game/gui/boss/dawn-of-time/boss103/boss103-inflamed.png",
            //   JSImport.Default
            // )
            // object inflamed extends Asset
            val inflamed = Asset("assets/in-game/gui/boss/dawn-of-time/boss103/boss103-inflamed.png")
            // @js.native @JSImport(
            //   "assets/in-game/gui/boss/dawn-of-time/boss103/sacred-ground.png",
            //   JSImport.Default
            // )
            // object sacredGroundArea extends Asset
            val sacredGroundArea = Asset("assets/in-game/gui/boss/dawn-of-time/boss103/sacred-ground.png")
          }
        }
      }

      object `default-abilities` {
        // @js.native @JSImport(
        //   "assets/in-game/gui/default-abilities/players/square-shield.png",
        //   JSImport.Default
        // )
        // object squareShield extends Asset
        val squareShield = Asset("assets/in-game/gui/default-abilities/players/square-shield.png")

        // @js.native @JSImport(
        //   "assets/in-game/gui/default-abilities/players/rage-filler.png",
        //   JSImport.Default
        // )
        // object rageFiller extends Asset
        val rageFiller = Asset("assets/in-game/gui/default-abilities/players/rage-filler.png")

        // @js.native @JSImport(
        //   "assets/in-game/gui/default-abilities/players/energy-filler.png",
        //   JSImport.Default
        // )
        // object energyFiller extends Asset
        val energyFiller = Asset("assets/in-game/gui/default-abilities/players/energy-filler.png")

        // @js.native @JSImport(
        //   "assets/in-game/gui/default-abilities/players/mana-filler.png",
        //   JSImport.Default
        // )
        // object manaFiller extends Asset
        val manaFiller = Asset("assets/in-game/gui/default-abilities/players/mana-filler.png")
      }

      object markers {
        val markerCross    = Asset("assets/in-game/gui/markers/marker-cross.png")
        val markerLozenge  = Asset("assets/in-game/gui/markers/marker-lozenge.png")
        val markerMoon     = Asset("assets/in-game/gui/markers/marker-moon.png")
        val markerSquare   = Asset("assets/in-game/gui/markers/marker-square.png")
        val markerStar     = Asset("assets/in-game/gui/markers/marker-star.png")
        val markerTriangle = Asset("assets/in-game/gui/markers/marker-triangle.png")
      }
    }
  }

  // implicit def assetAsString(asset: Asset): String =
  //   Try(asset.asInstanceOf[String]).fold(_ => throw new AssetNotProperlyDefined(asset), identity[String])
  implicit def assetAsString(asset: Asset): String = asset.name

  val abilityAssetMap: Map[Ability.AbilityId, Asset] = Map(
    Ability.hexagonHexagonHotId -> ingame.gui.abilities.hexagonHot,
    Ability.hexagonFlashHealId -> ingame.gui.abilities.hexagonFlashHeal,
    Ability.squareHammerHit -> ingame.gui.abilities.squareHammerHit,
    Ability.squareTauntId -> ingame.gui.abilities.squareTaunt,
    Ability.squareEnrageId -> ingame.gui.abilities.squareEnrage,
    Ability.squareCleaveId -> ingame.gui.abilities.squareCleave,
    Ability.triangleDirectHit -> ingame.gui.abilities.triangleDirectHit,
    Ability.triangleUpgradeDirectHit -> ingame.gui.abilities.triangleUpgradeDirectHit,
    Ability.triangleStun -> ingame.gui.abilities.triangleStun,
    Ability.pentagonPentagonBullet -> ingame.gui.abilities.pentagonBullet,
    Ability.createPentagonZoneId -> ingame.gui.abilities.pentagonZone,
    Ability.pentagonDispelId -> ingame.gui.abilities.pentagonDispel
  )

  val buffAssetMap: Map[Buff.ResourceIdentifier, Asset] = Map(
    Buff.hexagonHotIdentifier -> ingame.gui.abilities.hexagonHot,
    Buff.boss101BigDotIdentifier -> ingame.gui.abilities.boss101BigDot,
    Buff.squareDefaultShield -> ingame.gui.`default-abilities`.squareShield,
    Buff.rageFiller -> ingame.gui.`default-abilities`.rageFiller,
    Buff.energyFiller -> ingame.gui.`default-abilities`.energyFiller,
    Buff.manaFiller -> ingame.gui.`default-abilities`.manaFiller,
    Buff.triangleUpgradeDirectHit -> ingame.gui.abilities.triangleUpgradeDirectHit,
    Buff.triangleStun -> ingame.gui.abilities.triangleStun,
    Buff.squareEnrage -> ingame.gui.abilities.squareEnrage,
    Buff.boss102LivingDamageZone -> ingame.gui.boss.dawnOfTime.boss102.livingDamageZone,
    Buff.boss103Punished -> ingame.gui.boss.dawnOfTime.boss103.punished,
    Buff.boss103Purified -> ingame.gui.boss.dawnOfTime.boss103.purified,
    Buff.boss103Inflamed -> ingame.gui.boss.dawnOfTime.boss103.inflamed
  )

  val markerAssetMap: Map[GameMarker, Asset] = Map(
    GameMarker.Cross -> ingame.gui.markers.markerCross,
    GameMarker.Lozenge -> ingame.gui.markers.markerLozenge,
    GameMarker.Moon -> ingame.gui.markers.markerMoon,
    GameMarker.Square -> ingame.gui.markers.markerSquare,
    GameMarker.Star -> ingame.gui.markers.markerStar,
    GameMarker.Triangle -> ingame.gui.markers.markerTriangle
  )

}
