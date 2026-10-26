package bffclient.render

import bffclient.love.{Graphics, Image, Quad}
import bffclient.lua.Images
import gamelogic.abilities.Ability
import gamelogic.buffs.Buff

/** An image, or a sheet of `rows` x `columns` animation frames. */
final class Sprite(image: Image, rows: Int = 1, columns: Int = 1) {
  val frameWidth: Double  = image.getWidth() / columns
  val frameHeight: Double = image.getHeight() / rows

  private val frames: Vector[Quad] = for
    row    <- (0 until rows).toVector
    column <- 0 until columns
  yield Graphics.newQuad(
    column * frameWidth,
    row * frameHeight,
    frameWidth,
    frameHeight,
    image.getWidth(),
    image.getHeight()
  )

  def frameCount: Int = frames.size

  /** Draws the frame centred at (x, y) on screen, turned by `rotation` (counterclockwise, like in
    * the game), `width` pixels wide (and as high as the frame's proportions say, unless given). The
    * current colour tints it.
    */
  def draw(
      x: Double,
      y: Double,
      rotation: Double,
      width: Double,
      height: Double = -1,
      frame: Int = 0
  ): Unit = {
    val scaleX = width / frameWidth
    val scaleY = if height < 0 then scaleX else height / frameHeight
    Graphics.draw(
      image,
      frames(frame % frames.size),
      x,
      y,
      -rotation,
      scaleX,
      scaleY,
      frameWidth / 2,
      frameHeight / 2
    )
  }
}

/** The frontend's images (copied from the BFF checkout by `bffClient/luaLink`), when they exist. */
object Sprites {
  private def load(path: String, rows: Int = 1, columns: Int = 1): Option[Sprite] =
    Option(Images.load(s"assets/$path")).map(Sprite(_, rows, columns))

  /** By class name (`PlayerClasses.value`): white shapes, tinted with the player's colour. */
  lazy val playerClasses: Map[String, Sprite] =
    Seq("Triangle", "Square", "Pentagon", "Hexagon").flatMap { name =>
      load(s"players/${name.toLowerCase}.png").map(name -> _)
    }.toMap

  /** Sparks in the hands of a player casting an ability (6 x 5 frames, tinted with its colour). */
  lazy val casting: Option[Sprite] =
    load("abilities/player-casting-ability-animation.png", rows = 6, columns = 5)

  /** The square's cleave (3 frames). */
  lazy val cleave: Option[Sprite] = load("abilities/cleave-animation.png", columns = 3)

  /** The sword of the triangle's direct hit. */
  lazy val sword: Option[Sprite] = load("abilities/sword.png")

  /** Boss 102's damage zones (12 frames) and the zone around the players carrying one (3 x 5
    * frames).
    */
  lazy val damageZone: Option[Sprite] =
    load("boss/dawn-of-time/boss102/damage-zone-animation.png", columns = 12)
  lazy val livingDamageZone: Option[Sprite] =
    load("boss/dawn-of-time/boss102/living-damage-zone-animation.png", rows = 3, columns = 5)

  /** The arena's background (2000 x 2000). */
  lazy val aztecDiamond: Option[Sprite] = load("background/aztec-diamond.png")

  /** On the target frame, when we face our target. */
  lazy val smallLock: Option[Sprite] = load("misc/small-lock.png")

  /** The icons of the buffs, like the frontend's `Asset.buffAssetMap`, without the permanent buffs
    * of the classes (the square's shield, the resource fillers), which every player of the class
    * has, and without Boss 110's.
    */
  lazy val buffIcons: Map[Buff.ResourceIdentifier, Sprite] = Seq(
    Buff.hexagonHotIdentifier     -> "abilities/hexagon-hot",
    Buff.boss101BigDotIdentifier  -> "abilities/boss101-big-dot",
    Buff.triangleUpgradeDirectHit -> "abilities/triangle-upgrade-direct-hit",
    Buff.triangleStun             -> "abilities/triangle-stun",
    Buff.silence                  -> "abilities/triangle-cut",
    Buff.squareEnrage             -> "abilities/square-enrage",
    Buff.boss102LivingDamageZone  -> "boss/dawn-of-time/boss102/living-damage-zone",
    Buff.sirOfVicePunished        -> "boss/first-raid/sir-of-vice/punished",
    Buff.sirOfVicePurified        -> "boss/first-raid/sir-of-vice/purified",
    Buff.sirOfViceInflamed        -> "boss/first-raid/sir-of-vice/inflamed",
    Buff.boss104TwinDebuff        -> "boss/dawn-of-time/boss104/twin-debuff",
    Buff.boss105VolatileCharge    -> "boss/dawn-of-time/boss105/volatile-charge",
    Buff.boss105TwinBond          -> "boss/dawn-of-time/boss105/twin-bond",
    Buff.boss105Mourning          -> "boss/dawn-of-time/boss105/mourning",
    Buff.boss105Yearning          -> "boss/dawn-of-time/boss105/yearning"
  ).flatMap((id, icon) => load(s"$icon.png").map(id -> _)).toMap

  /** The icons of the abilities, like the frontend's `Asset.abilityAssetMap`. */
  lazy val abilityIcons: Map[Ability.AbilityId, Sprite] = Seq(
    Ability.hexagonHexagonHotId      -> "hexagon-hot",
    Ability.hexagonFlashHealId       -> "hexagon-flash-heal",
    Ability.hexagonHexagonZoneId     -> "create-hexagon-zone",
    Ability.squareHammerHit          -> "square-hammer-hit",
    Ability.squareTauntId            -> "square-taunt",
    Ability.squareEnrageId           -> "square-enrage",
    Ability.squareCleaveId           -> "square-cleave",
    Ability.triangleEnergyKick       -> "triangle-energy-kick",
    Ability.triangleDirectHit        -> "triangle-direct-hit",
    Ability.triangleUpgradeDirectHit -> "triangle-upgrade-direct-hit",
    Ability.triangleStun             -> "triangle-stun",
    Ability.triangleCut              -> "triangle-cut",
    Ability.pentagonPentagonBullet   -> "pentagon-bullet",
    Ability.createPentagonZoneId     -> "create-pentagon-zone",
    Ability.pentagonDispelId         -> "pentagon-dispel"
  ).flatMap((id, icon) => load(s"abilities/$icon.png").map(id -> _)).toMap
}
