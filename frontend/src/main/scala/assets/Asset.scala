package assets

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.gameextras.GameMarker
import urldsl.language.dummyErrorImpl.*
import urldsl.language.PathSegment

import scala.language.implicitConversions
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try
import models.bff.outofgame.PlayerClasses

class Asset private (val path: PathSegment[Unit, ?], val name: String, width: Int, height: Int)
    extends IndigoLikeAsset {

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
    indigo.AssetPath(pathStr)
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

  private lazy val pathStr = "/" ++ path.createPart()

}

object Asset {

  private val _allAssets = mutable.Set.empty[Asset]

  def all: Set[Asset] = _allAssets.toSet

  private def apply(path: PathSegment[Unit, ?], width: Int, height: Int): Asset = {
    val asset = new Asset(
      path,
      path.createSegments().last.content.reverse.dropWhile(_ != '.').tail.reverse,
      width,
      height
    )
    _allAssets += asset
    asset
  }

  private val basePath = services.routing.base / "assets"

  object ingame {

    private val ingameP = basePath / "in-game"

    object gui {

      private val guiP = ingameP / "gui"

      object abilities {
        private val abilitiesP = guiP / "abilities"

        val abilityOverlay   = Asset(abilitiesP / "ability-overlay.png", 30, 30)
        val hexagonFlashHeal = Asset(abilitiesP / "hexagon-flash-heal.png", 30, 30)
        val hexagonHot       = Asset(abilitiesP / "hexagon-hot.png", 30, 30)
        val squareHammerHit  = Asset(abilitiesP / "square-hammer-hit.png", 30, 30)
        val squareTaunt      = Asset(abilitiesP / "square-taunt.png", 30, 30)
        val squareEnrage     = Asset(abilitiesP / "square-enrage.png", 32, 32)
        val squareCleave     = Asset(abilitiesP / "square-cleave.png", 32, 32)
        val triangleDirectHit =
          Asset(abilitiesP / "triangle-direct-hit.png", 30, 30)
        val triangleUpgradeDirectHit = Asset(abilitiesP / "triangle-upgrade-direct-hit.png", 32, 32)
        val triangleStun             = Asset(abilitiesP / "triangle-stun.png", 32, 32)
        val triangleEnergyKick =
          Asset(abilitiesP / "triangle-energy-kick.png", 32, 32)
        val pentagonBullet = Asset(abilitiesP / "pentagon-bullet.png", 32, 32)
        val pentagonZone   = Asset(abilitiesP / "create-pentagon-zone.png", 32, 32)
        val pentagonDispel = Asset(abilitiesP / "pentagon-dispel.png", 32, 32)
        val boss101BigDot  = Asset(abilitiesP / "boss101-big-dot.png", 30, 30)

        val triangleDirectHitEffect = Asset(abilitiesP / "sword.png", 20, 2)
        val cleaveEffect            = Asset(abilitiesP / "cleave-animation.png", 135, 40)

        val playerCastingAbilityAnimation =
          Asset(abilitiesP / "player-casting-ability-animation.png", 240, 288)

      }

      object background {
        private val backgroundP = guiP / "background"

        val anAztecDiamond = Asset(backgroundP / "aztec-diamond.png", 2000, 2000)
      }

      object bars {
        private val barsP = guiP / "bars"

        val liteStepBar     = Asset(barsP / "LiteStep_wenakari.png", 256, 32)
        val lifeBarWenakari = Asset(barsP / "life-bar_wenakari.png", 373, 51)
        val xeonBar         = Asset(barsP / "Xeon.png", 256, 32)
        val minimalist      = Asset(barsP / "Minimalist.png", 256, 32)

        def allBars = Vector(liteStepBar, lifeBarWenakari, xeonBar, minimalist)
      }

      object boss {
        private val bossP = guiP / "boss"

        object dawnOfTime {
          private val dawnOfTimeP = bossP / "dawn-of-time"

          object boss102 {
            private val boss102P = dawnOfTimeP / "boss102"

            val livingDamageZone = Asset(boss102P / "living-damage-zone.png", 32, 32)
            val livingDamageZoneAnimation =
              Asset(boss102P / "living-damage-zone-animation.png", 960, 576)
            val damageZoneAnimation =
              Asset(boss102P / "damage-zone-animation.png", 768, 64)
          }
          object boss103 {
            private val boss103P = dawnOfTimeP / "boss103"

            val punished         = Asset(boss103P / "boss103-punished.png", 32, 32)
            val purified         = Asset(boss103P / "boss103-purified.png", 32, 32)
            val inflamed         = Asset(boss103P / "boss103-inflamed.png", 32, 32)
            val sacredGroundArea = Asset(boss103P / "sacred-ground.png", 500, 500)
          }

          object boss110 {
            private val boss110P = dawnOfTimeP / "boss110"

            val brokenArmor = Asset(boss110P / "broken-armor.png", 32, 32)
            val bigGuy      = Asset(boss110P / "big-guy.png", 64, 64)
            val smallGuy    = Asset(boss110P / "small-guy.png", 64, 64)
            val bombPod     = Asset(boss110P / "bomb-pod.png", 64, 64)
          }
        }
      }

      object `default-abilities` {
        private val defaultAbilitiesP = guiP / "default-abilities"
        private val playersP          = defaultAbilitiesP / "players"

        val squareShield = Asset(playersP / "square-shield.png", 30, 30)
        val rageFiller   = Asset(playersP / "rage-filler.png", 30, 30)
        val energyFiller = Asset(playersP / "energy-filler.png", 30, 30)
        val manaFiller   = Asset(playersP / "mana-filler.png", 32, 32)
      }

      object markers {
        private val markersP = guiP / "markers"

        val markerCross    = Asset(markersP / "marker-cross.png", 64, 64)
        val markerLozenge  = Asset(markersP / "marker-lozenge.png", 64, 64)
        val markerMoon     = Asset(markersP / "marker-moon.png", 64, 64)
        val markerSquare   = Asset(markersP / "marker-square.png", 64, 64)
        val markerStar     = Asset(markersP / "marker-star.png", 64, 64)
        val markerTriangle = Asset(markersP / "marker-triangle.png", 64, 64)
      }

      object players {
        private val playersP = guiP / "players"

        val triangle = Asset(playersP / "triangle.png", 50, 50)
        val square   = Asset(playersP / "square.png", 50, 50)
        val pentagon = Asset(playersP / "pentagon.png", 50, 50)
        val hexagon  = Asset(playersP / "hexagon.png", 50, 50)
      }

      object misc {
        private val miscP = guiP / "misc"

        val smallLock = Asset(miscP / "small-lock.png", 7, 11)
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

  val background = ingame.gui.background

  val bars = ingame.gui.bars.allBars

  val misc = ingame.gui.misc

  val boss102Animations = Vector(
    ingame.gui.boss.dawnOfTime.boss102.damageZoneAnimation,
    ingame.gui.boss.dawnOfTime.boss102.livingDamageZoneAnimation
  )

}
