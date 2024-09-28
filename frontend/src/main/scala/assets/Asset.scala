package assets

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.gameextras.GameMarker

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try
import models.bff.outofgame.PlayerClasses

class Asset(val path: String, val name: String, width: Int, height: Int) {

  override final def equals(obj: Any): Boolean = obj match {
    case that: Asset => this.name == that.name
    case _           => false
  }

  override final def hashCode(): Int = name.hashCode()

  val assetName = indigo.AssetName(name)

  val size: indigo.Size    = indigo.Size(width, height)
  val center: indigo.Point = indigo.Point(width / 2, height / 2)

  def scaleTo(targetSize: indigo.Size): indigo.Vector2 =
    indigo.Vector2(targetSize.width / width.toDouble, targetSize.height / height.toDouble)
  def scaleTo(targetSize: Double): indigo.Vector2 =
    indigo.Vector2(targetSize / width, targetSize / height)

  def asIndigoAssetType: indigo.AssetType = indigo.AssetType.Image(
    assetName,
    indigo.AssetPath(path)
  )

  def indigoGraphic(
      position: indigo.Point,
      maybeTint: Option[indigo.RGBA],
      rotation: indigo.Radians,
      targetSize: indigo.Size
  ) = indigo
    .Graphic(
      indigo.Rectangle(size),
      1,
      maybeTint match {
        case None => indigo.Material.Bitmap(assetName)
        case Some(tint) =>
          indigo.Material
            .ImageEffects(assetName)
            .withTint(tint)
      }
    )
    .withPosition(position)
    .withRef(center)
    .withRotation(rotation)
    .withScale(scaleTo(targetSize))

}

object Asset {

  def apply(path: String, width: Int, height: Int): Asset =
    new Asset(path, path.split("/").last.reverse.dropWhile(_ != '.').tail.reverse, width, height)

  def allAssets: Set[Asset] =
    Set(
      ingame.gui.abilities.abilityOverlay,
      ingame.gui.boss.dawnOfTime.boss103.sacredGroundArea,
      ingame.gui.abilities.triangleDirectHitEffect,
      ingame.gui.abilities.cleaveEffect
    ) ++
      Asset.ingame.gui.bars.allBars ++
      Asset.markerAssetMap.values ++
      Asset.buffAssetMap.values ++
      Asset.abilityAssetMap.values ++
      Asset.units ++ Asset.playerClassAssetMap.values

  final class AssetNotProperlyDefined(asset: Asset)
      extends Exception(
        s"The asset `${asset.toString}` does not seem to work. Did you forget to restart the Dev server? Or Perhaps" +
          s"you forgot to add the asset in resource path? A typo in the filepath could also cause that."
      )

  object ingame {
    object gui {
      object abilities {
        val abilityOverlay   = Asset("/assets/in-game/gui/abilities/ability-overlay.png", 30, 30)
        val hexagonFlashHeal = Asset("/assets/in-game/gui/abilities/hexagon-flash-heal.png", 30, 30)
        val hexagonHot       = Asset("/assets/in-game/gui/abilities/hexagon-hot.png", 30, 30)
        val squareHammerHit  = Asset("/assets/in-game/gui/abilities/square-hammer-hit.png", 30, 30)
        val squareTaunt      = Asset("/assets/in-game/gui/abilities/square-taunt.png", 30, 30)
        val squareEnrage     = Asset("/assets/in-game/gui/abilities/square-enrage.png", 32, 32)
        val squareCleave     = Asset("/assets/in-game/gui/abilities/square-cleave.png", 32, 32)
        val triangleDirectHit =
          Asset("/assets/in-game/gui/abilities/triangle-direct-hit.png", 30, 30)
        val triangleUpgradeDirectHit = Asset(
          "/assets/in-game/gui/abilities/triangle-upgrade-direct-hit.png",
          32,
          32
        )
        val triangleStun = Asset("/assets/in-game/gui/abilities/triangle-stun.png", 32, 32)
        val triangleEnergyKick =
          Asset("/assets/in-game/gui/abilities/triangle-energy-kick.png", 32, 32)
        val pentagonBullet = Asset("/assets/in-game/gui/abilities/pentagon-bullet.png", 32, 32)
        val pentagonZone   = Asset("/assets/in-game/gui/abilities/create-pentagon-zone.png", 32, 32)
        val pentagonDispel = Asset("/assets/in-game/gui/abilities/pentagon-dispel.png", 32, 32)
        val boss101BigDot  = Asset("/assets/in-game/gui/abilities/boss101-big-dot.png", 30, 30)

        val triangleDirectHitEffect = Asset("/assets/in-game/gui/abilities/sword.png", 20, 2)
        val cleaveEffect = Asset("/assets/in-game/gui/abilities/cleave-animation.png", 135, 40)

      }

      object bars {
        val liteStepBar     = Asset("/assets/in-game/gui/bars/LiteStep_wenakari.png", 256, 32)
        val lifeBarWenakari = Asset("/assets/in-game/gui/bars/life-bar_wenakari.png", 373, 51)
        val xeonBar         = Asset("/assets/in-game/gui/bars/Xeon.png", 256, 32)
        val minimalist      = Asset("/assets/in-game/gui/bars/Minimalist.png", 256, 32)

        def allBars = Vector(liteStepBar, lifeBarWenakari, xeonBar, minimalist)
      }

      object boss {
        object dawnOfTime {
          object boss102 {
            val livingDamageZone = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss102/living-damage-zone.png",
              32,
              32
            )
          }
          object boss103 {
            val punished = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-punished.png",
              32,
              32
            )
            val purified = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-purified.png",
              32,
              32
            )
            val inflamed = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/boss103-inflamed.png",
              32,
              32
            )
            val sacredGroundArea = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss103/sacred-ground.png",
              500,
              500
            )
          }

          object boss110 {

            val brokenArmor = Asset(
              "/assets/in-game/gui/boss/dawn-of-time/boss110/broken-armor.png",
              32,
              32
            )
            val bigGuy = Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/big-guy.png", 64, 64)
            val smallGuy =
              Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/small-guy.png", 64, 64)
            val bombPod =
              Asset("/assets/in-game/gui/boss/dawn-of-time/boss110/bomb-pod.png", 64, 64)

          }
        }
      }

      object `default-abilities` {
        val squareShield =
          Asset("/assets/in-game/gui/default-abilities/players/square-shield.png", 30, 30)
        val rageFiller =
          Asset("/assets/in-game/gui/default-abilities/players/rage-filler.png", 30, 30)
        val energyFiller =
          Asset("/assets/in-game/gui/default-abilities/players/energy-filler.png", 30, 30)
        val manaFiller =
          Asset("/assets/in-game/gui/default-abilities/players/mana-filler.png", 32, 32)
      }

      object markers {
        val markerCross    = Asset("/assets/in-game/gui/markers/marker-cross.png", 64, 64)
        val markerLozenge  = Asset("/assets/in-game/gui/markers/marker-lozenge.png", 64, 64)
        val markerMoon     = Asset("/assets/in-game/gui/markers/marker-moon.png", 64, 64)
        val markerSquare   = Asset("/assets/in-game/gui/markers/marker-square.png", 64, 64)
        val markerStar     = Asset("/assets/in-game/gui/markers/marker-star.png", 64, 64)
        val markerTriangle = Asset("/assets/in-game/gui/markers/marker-triangle.png", 64, 64)
      }

      object players {
        val triangle = Asset("/assets/in-game/gui/players/triangle.png", 50, 50)
        val square   = Asset("/assets/in-game/gui/players/square.png", 50, 50)
        val pentagon = Asset("/assets/in-game/gui/players/pentagon.png", 50, 50)
        val hexagon  = Asset("/assets/in-game/gui/players/hexagon.png", 50, 50)
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

  val playerClassAssetMap: Map[PlayerClasses, Asset] = Map(
    PlayerClasses.Triangle -> ingame.gui.players.triangle,
    PlayerClasses.Square   -> ingame.gui.players.square,
    PlayerClasses.Pentagon -> ingame.gui.players.pentagon,
    PlayerClasses.Hexagon  -> ingame.gui.players.hexagon
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
