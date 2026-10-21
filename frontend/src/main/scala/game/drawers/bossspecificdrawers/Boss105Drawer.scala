package game.drawers.bossspecificdrawers

import game.drawers.{minicastingbar, minilifebar}
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Siphon, Skewer}
import gamelogic.buffs.boss.boss105.{Mourning, TwinBond, VolatileCharge, Yearning}
import gamelogic.entities.boss.boss105.{ReformationPoint, Shade}
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import indigo.*
import indigo.shared.scenegraph.Layer.Content

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/** Everything Boss105 needs on screen:
  *   - the Shade, and the bond between the twins: an aura around each of them, which turn red when
  *     they touch, because that is when the twins resonate;
  *   - the beam of [[Skewer]] while it is being cast, which follows the target;
  *   - the explosion radius of the [[VolatileCharge]]s, filling up as the charge gets closer to
  *     exploding;
  *   - the ring of the boss: cyan when it is bonded to its Shade, orange when it is exposed;
  *   - the place where the Shade will come back, and how soon;
  *   - the call of the Shade ([[Reunion]]) and the drain of the [[Siphon]].
  */
object Boss105Drawer extends game.drawers.DrawerWithCloneBlanks {

  private val bondColour      = RGBA.fromColorInts(80, 220, 240)
  private val exposedColour   = RGBA.fromColorInts(255, 150, 30)
  private val shadeColour     = RGBA.fromColorInts(90, 40, 140)
  private val resonanceColour = RGBA.fromColorInts(230, 30, 60)
  private val beamColour      = RGBA.fromColorInts(255, 60, 60)
  private val chargeColour    = RGBA.fromColorInts(255, 110, 20)
  private val siphonColour    = RGBA.fromColorInts(120, 220, 90)

  override def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point): Content =
    Content.empty

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.bosses.values.toJSArray.collect { case boss: Boss105 => boss }.flatMap { boss =>
      drawBondAndAuras(gameState, boss, now, gameToLocal) ++
        drawBossRing(gameState, boss, now, gameToLocal) ++
        drawBeam(gameState, boss, gameToLocal) ++
        drawReunionCall(gameState, boss, gameToLocal)
    } ++ drawCharges(gameState, gameToLocal) ++ drawReformationPoints(gameState, gameToLocal) ++
      drawShades(gameState, gameToLocal)

  private def castingAbility(gameState: GameState, casterId: gamelogic.entities.Entity.Id) =
    gameState.castingEntityInfo.get(casterId).filter(_.isCasting(gameState.time, 0L))

  private def progress(info: gamelogic.entities.EntityCastingInfo, time: Long): Double =
    1.0 - info.remainingCastingTime(time, 0L).toDouble / info.castingTime max 0.0 min 1.0

  /** Circles of half the resonance range around each twin: they overlap exactly when the twins are
    * closer than the resonance range. When that happens, they turn red and a red line links the
    * twins.
    */
  private def drawBondAndAuras(
      gameState: GameState,
      boss: Boss105,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.allTEntities[Shade].values.toJSArray.flatMap { shade =>
      val bossPosition  = boss.currentPosition(gameState.time)
      val shadePosition = shade.currentPosition(gameState.time)
      val resonating    = bossPosition.distanceTo(shadePosition) < Resonance.range
      val colour        = if resonating then resonanceColour else shadeColour
      val pulse         = if resonating then 0.12 + 0.08 * math.sin(now / 80.0) else 0.0
      val auraRadius    = (Resonance.range / 2).toInt

      def aura(centre: Complex): SceneNode =
        Shape
          .Circle(
            gameToLocal(centre),
            auraRadius,
            Fill.Color(colour.withAlpha(0.05 + pulse)),
            Stroke(2, colour.withAlpha(if resonating then 0.9 else 0.35))
          )
          .withDepth(Depth.far)

      val link = Shape
        .Line(
          gameToLocal(bossPosition),
          gameToLocal(shadePosition),
          Stroke(if resonating then 5 else 2, colour.withAlpha(if resonating then 0.9 else 0.3))
        )
        .withDepth(Depth.far)

      js.Array[SceneNode](aura(bossPosition), aura(shadePosition), link)
    }

  /** Cyan ring: the boss takes only a fraction of the damage. Orange ring, pulsing: the boss is
    * exposed and frenzied.
    */
  private def drawBossRing(
      gameState: GameState,
      boss: Boss105,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = {
    val centre = gameToLocal(boss.currentPosition(gameState.time))
    val radius = boss.shape.radius.toInt + 6

    if gameState.hasBuffOfType[Mourning](boss.id) then
      js.Array(
        Shape
          .Circle(
            centre,
            radius + (3 * math.sin(now / 120.0)).toInt,
            Fill.Color(exposedColour.withAlpha(0.2)),
            Stroke(5, exposedColour)
          )
          .withDepth(Depth.far)
      )
    else if gameState.hasBuffOfType[TwinBond](boss.id) then
      js.Array(
        Shape
          .Circle(centre, radius, Fill.Color(RGBA.Zero), Stroke(4, bondColour.withAlpha(0.9)))
          .withDepth(Depth.far)
      )
    else js.Array()
  }

  /** The beam of the Skewer: from the boss, through its target, up to the wall. It gets more and
    * more opaque until it is fired.
    */
  private def drawBeam(
      gameState: GameState,
      boss: Boss105,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    (for {
      info   <- castingAbility(gameState, boss.id)
      skewer <- Some(info.ability).collect { case skewer: Skewer => skewer }
      target <- gameState.players.get(skewer.targetId)
      source    = boss.currentPosition(gameState.time)
      through   = target.currentPosition(gameState.time)
      direction = (through - source).safeNormalized
      if direction.modulus2 > 0
    } yield {
      val length = distanceToWall(source, direction)
      val side   = direction.orthogonal * Skewer.halfWidth
      val end    = source + direction * length
      val alpha  = 0.12 + 0.35 * progress(info, gameState.time)
      // positively oriented in the game, hence reversed on screen (see Boss104Drawer)
      val corners = js.Array(source - side, end - side, end + side, source + side).reverse
      js.Array[SceneNode](
        Shape
          .Polygon(
            Batch(corners.map(gameToLocal)),
            fill = Fill.Color(beamColour.withAlpha(alpha)),
            stroke = Stroke(2, beamColour.withAlpha(0.8))
          )
          .withDepth(Depth.far)
      )
    }).getOrElse(js.Array())

  /** Distance from `source` to the walls of the room, going in `direction`. */
  private def distanceToWall(source: Complex, direction: Complex): Double = {
    def toWall(position: Double, speed: Double): Double =
      if speed > 1e-9 then (Boss105.size - position) / speed
      else if speed < -1e-9 then (-Boss105.size - position) / speed
      else Double.MaxValue

    (toWall(source.re, direction.re) min toWall(source.im, direction.im)) max 0.0
  }

  /** While the boss casts the Reunion, and then while the Shade is on its way, a line shows what is
    * about to happen.
    */
  private def drawReunionCall(
      gameState: GameState,
      boss: Boss105,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = {
    val maybeAlpha = castingAbility(gameState, boss.id)
      .filter(_.ability.isInstanceOf[Reunion])
      .map(info => 0.2 + 0.5 * progress(info, gameState.time))
      .orElse(
        Option
          .when(gameState.allTEntities[Shade].keys.exists(gameState.hasBuffOfType[Yearning](_)))(
            0.7
          )
      )

    (for {
      alpha <- maybeAlpha
      shade <- gameState.allTEntities[Shade].values.headOption
    } yield js.Array[SceneNode](
      Shape
        .Line(
          gameToLocal(boss.currentPosition(gameState.time)),
          gameToLocal(shade.currentPosition(gameState.time)),
          Stroke(6, shadeColour.withAlpha(alpha))
        )
        .withDepth(Depth.far)
    )).getOrElse(js.Array())
  }

  /** The explosion radius of each charge, and the part of it that the explosion has already
    * reached: when the inner disc touches the ring, it explodes.
    */
  private def drawCharges(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.allTBuffs[VolatileCharge].toJSArray.flatMap { charge =>
      gameState.players.get(charge.bearerId).toJSArray.flatMap { bearer =>
        val centre = gameToLocal(bearer.currentPosition(gameState.time))
        val elapsed =
          ((gameState.time - charge.appearanceTime).toDouble / VolatileCharge.duration max 0.0) min 1.0
        js.Array[SceneNode](
          Shape
            .Circle(
              centre,
              VolatileCharge.radius.toInt,
              Fill.Color(chargeColour.withAlpha(0.06)),
              Stroke(2, chargeColour.withAlpha(0.8))
            )
            .withDepth(Depth.far),
          Shape
            .Circle(
              centre,
              (VolatileCharge.radius * elapsed).toInt max 1,
              Fill.Color(chargeColour.withAlpha(0.3)),
              Stroke(0, RGBA.Zero)
            )
            .withDepth(Depth.far)
        )
      }
    }

  /** Where the Shade will come back: a ghost of the Shade, which becomes more solid until it is
    * back.
    */
  private def drawReformationPoints(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.allTEntities[ReformationPoint].values.toJSArray.flatMap { point =>
      val elapsed = gameState
        .allTBuffs[Mourning]
        .headOption
        .fold(0.0)(m => ((gameState.time - m.appearanceTime).toDouble / Mourning.duration) min 1.0)
      js.Array[SceneNode](
        Shape
          .Circle(
            gameToLocal(point.pos),
            ReformationPoint.radius.toInt,
            Fill.Color(shadeColour.withAlpha(0.1 + 0.3 * elapsed)),
            Stroke(2, shadeColour.withAlpha(0.8))
          )
          .withDepth(Depth.far),
        Shape
          .Polygon(
            Batch(
              Shade.shape.vertices.toJSArray.reverse.map(vertex => gameToLocal(point.pos + vertex))
            ),
            fill = Fill.Color(shadeColour.withAlpha(0.15 + 0.4 * elapsed)),
            stroke = Stroke(2, shadeColour)
          )
          .withDepth(Depth.far)
      )
    }

  private def drawShades(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.allTEntities[Shade].values.toJSArray.flatMap { shade =>
      val shadePosition = shade.currentPosition(gameState.time)
      val barPos        = gameToLocal(shadePosition + Complex.i * (shade.shape.radius + 8))
      val yearning      = gameState.hasBuffOfType[Yearning](shade.id)

      val siphonLine = castingAbility(gameState, shade.id)
        .flatMap(info =>
          Some(info.ability)
            .collect { case siphon: Siphon => siphon }
            .flatMap(siphon => gameState.players.get(siphon.targetId).map(info -> _))
        )
        .map((info, target) =>
          Shape
            .Line(
              gameToLocal(shadePosition),
              gameToLocal(target.currentPosition(gameState.time)),
              Stroke(3, siphonColour.withAlpha(0.2 + 0.6 * progress(info, gameState.time)))
            )
            .withDepth(Depth.far): SceneNode
        )
        .toJSArray

      js.Array[SceneNode](
        Shape
          .Polygon(
            Batch(
              shade.shape.vertices.toJSArray.reverse
                .map(_ * Complex.rotation(shade.rotation) + shadePosition)
                .map(gameToLocal)
            ),
            fill = Fill.Color(shadeColour.withAlpha(0.55)),
            stroke = Stroke(if yearning then 4 else 2, if yearning then RGBA.White else RGBA.Black)
          )
          .withDepth(Depth(4))
      ) ++ siphonLine ++
        minilifebar(shade, barPos).presentWithChildrenWithoutRectangle ++
        minicastingbar(shade, barPos - Point(0, 5), gameState).presentWithChildrenWithoutRectangle
    }

}
