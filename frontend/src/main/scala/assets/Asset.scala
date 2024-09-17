package assets

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.gameextras.GameMarker

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try

class Asset(val path: String, val name: String) {

  override final def equals(obj: Any): Boolean = obj match {
    case that: Asset => this.name == that.name
    case _           => false
  }

  override final def hashCode(): Int = name.hashCode()

  val assetName = indigo.AssetName(name)

  def asIndigoAssetType: indigo.AssetType = indigo.AssetType.Image(
    assetName,
    indigo.AssetPath(path)
  )
}

object Asset {

  def apply(path: String): Asset =
    new Asset(path, path.split("/").last.reverse.dropWhile(_ != '.').tail.reverse)

  def allAssets: Set[Asset] =
    Set(
      ingame.gui.bars.xeonBar,
      ingame.gui.bars.liteStepBar,
      ingame.gui.bars.minimalistBar,
      ingame.gui.abilities.abilityOverlay,
      ingame.gui.boss.dawnOfTime.boss103.sacredGroundArea
    ) ++
      Asset.markerAssetMap.values ++
      Asset.buffAssetMap.values ++
      Asset.abilityAssetMap.values ++
      Asset.units

  final class AssetNotProperlyDefined(asset: Asset)
      extends Exception(
        s"The asset `${asset.toString}` does not seem to work. Did you forget to restart the Dev server? Or Perhaps" +
          s"you forgot to add the asset in resource path? A typo in the filepath could also cause that."
      )

  object ingame {
    object gui {
      object abilities {
        val abilityOverlay    = Asset("/assets/in-game/gui/abilities/ability-overlay.png")
        val hexagonFlashHeal  = Asset("/assets/in-game/gui/abilities/hexagon-flash-heal.png")
        val hexagonHot        = Asset("/assets/in-game/gui/abilities/hexagon-hot.png")
        val squareHammerHit   = Asset("/assets/in-game/gui/abilities/square-hammer-hit.png")
        val squareTaunt       = Asset("/assets/in-game/gui/abilities/square-taunt.png")
        val squareEnrage      = Asset("/assets/in-game/gui/abilities/square-enrage.png")
        val squareCleave      = Asset("/assets/in-game/gui/abilities/square-cleave.png")
        val triangleDirectHit = Asset("/assets/in-game/gui/abilities/triangle-direct-hit.png")
        val triangleUpgradeDirectHit = Asset(
          "/assets/in-game/gui/abilities/triangle-upgrade-direct-hit.png"
        )
        val triangleStun       = Asset("/assets/in-game/gui/abilities/triangle-stun.png")
        val triangleEnergyKick = Asset("/assets/in-game/gui/abilities/triangle-energy-kick.png")
        val pentagonBullet     = Asset("/assets/in-game/gui/abilities/pentagon-bullet.png")
        val pentagonZone       = Asset("/assets/in-game/gui/abilities/create-pentagon-zone.png")
        val pentagonDispel     = Asset("/assets/in-game/gui/abilities/pentagon-dispel.png")
        val boss101BigDot      = Asset("/assets/in-game/gui/abilities/boss101-big-dot.png")

      }

      object bars {
        val liteStepBar   = Asset("/assets/in-game/gui/bars/LiteStep_wenakari.png")
        val minimalistBar = Asset("/assets/in-game/gui/bars/life-bar_wenakari.png")
        val xeonBar       = Asset("/assets/in-game/gui/bars/Xeon.png")
      }

      object boss {
        object dawnOfTime {
          object boss102 {
            val livingDamageZone = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss102/living-damage-zone.png"
            )
          }
          object boss103 {
            val punished = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-punished.png"
            )
            val purified = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-purified.png"
            )
            val inflamed = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-inflamed.png"
            )
            val sacredGroundArea = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/sacred-ground.png"
            )
          }

          object boss110 {

            val brokenArmor = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss110/broken-armor.png"
            )
            val bigGuy   = Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/big-guy.png")
            val smallGuy = Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/small-guy.png")
            val bombPod  = Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/bomb-pod.png")

          }
        }
      }

      object `default-abilities` {
        val squareShield = Asset("/assets/in-game/gui/default-abilities/players/square-shield.png")
        val rageFiller   = Asset("/assets/in-game/gui/default-abilities/players/rage-filler.png")
        val energyFiller = Asset("/assets/in-game/gui/default-abilities/players/energy-filler.png")
        val manaFiller   = Asset("/assets/in-game/gui/default-abilities/players/mana-filler.png")
      }

      object markers {
        val markerCross    = Asset("/assets/in-game/gui/markers/marker-cross.png")
        val markerLozenge  = Asset("/assets/in-game/gui/markers/marker-lozenge.png")
        val markerMoon     = Asset("/assets/in-game/gui/markers/marker-moon.png")
        val markerSquare   = Asset("/assets/in-game/gui/markers/marker-square.png")
        val markerStar     = Asset("/assets/in-game/gui/markers/marker-star.png")
        val markerTriangle = Asset("/assets/in-game/gui/markers/marker-triangle.png")
      }
    }
  }

  val abilityAssetMap: Map[Ability.AbilityId, Asset] = Map(
    Ability.hexagonHexagonHotId      -> ingame.gui.abilities.hexagonHot,
    Ability.hexagonFlashHealId       -> ingame.gui.abilities.hexagonFlashHeal,
    Ability.squareHammerHit          -> ingame.gui.abilities.squareHammerHit,
    Ability.squareTauntId            -> ingame.gui.abilities.squareTaunt,
    Ability.squareEnrageId           -> ingame.gui.abilities.squareEnrage,
    Ability.squareCleaveId           -> ingame.gui.abilities.squareCleave,
    Ability.triangleEnergyKick       -> ingame.gui.abilities.triangleEnergyKick,
    Ability.triangleDirectHit        -> ingame.gui.abilities.triangleDirectHit,
    Ability.triangleUpgradeDirectHit -> ingame.gui.abilities.triangleUpgradeDirectHit,
    Ability.triangleStun             -> ingame.gui.abilities.triangleStun,
    Ability.pentagonPentagonBullet   -> ingame.gui.abilities.pentagonBullet,
    Ability.createPentagonZoneId     -> ingame.gui.abilities.pentagonZone,
    Ability.pentagonDispelId         -> ingame.gui.abilities.pentagonDispel
  )

  val buffAssetMap: Map[Buff.ResourceIdentifier, Asset] = Map(
    Buff.hexagonHotIdentifier     -> ingame.gui.abilities.hexagonHot,
    Buff.boss101BigDotIdentifier  -> ingame.gui.abilities.boss101BigDot,
    Buff.squareDefaultShield      -> ingame.gui.`default-abilities`.squareShield,
    Buff.rageFiller               -> ingame.gui.`default-abilities`.rageFiller,
    Buff.energyFiller             -> ingame.gui.`default-abilities`.energyFiller,
    Buff.manaFiller               -> ingame.gui.`default-abilities`.manaFiller,
    Buff.triangleUpgradeDirectHit -> ingame.gui.abilities.triangleUpgradeDirectHit,
    Buff.triangleStun             -> ingame.gui.abilities.triangleStun,
    Buff.squareEnrage             -> ingame.gui.abilities.squareEnrage,
    Buff.boss102LivingDamageZone  -> ingame.gui.boss.dawnOfTime.boss102.livingDamageZone,
    Buff.boss103Punished          -> ingame.gui.boss.dawnOfTime.boss103.punished,
    Buff.boss103Purified          -> ingame.gui.boss.dawnOfTime.boss103.purified,
    Buff.boss103Inflamed          -> ingame.gui.boss.dawnOfTime.boss103.inflamed,
    Buff.boss110BrokenArmor       -> ingame.gui.boss.dawnOfTime.boss110.brokenArmor
  )

  val markerAssetMap: Map[GameMarker, Asset] = Map(
    GameMarker.Cross    -> ingame.gui.markers.markerCross,
    GameMarker.Lozenge  -> ingame.gui.markers.markerLozenge,
    GameMarker.Moon     -> ingame.gui.markers.markerMoon,
    GameMarker.Square   -> ingame.gui.markers.markerSquare,
    GameMarker.Star     -> ingame.gui.markers.markerStar,
    GameMarker.Triangle -> ingame.gui.markers.markerTriangle
  )

  val units: List[Asset] = List(
    ingame.gui.boss.dawnOfTime.boss110.bigGuy,
    ingame.gui.boss.dawnOfTime.boss110.bombPod,
    ingame.gui.boss.dawnOfTime.boss110.smallGuy
  )

}
