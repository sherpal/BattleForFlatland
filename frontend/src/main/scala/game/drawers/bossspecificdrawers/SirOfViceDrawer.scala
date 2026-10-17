package game.drawers.bossspecificdrawers

import gamelogic.abilities.boss.sirofvice.{CleansingNova, HolyWrath, SacredGround}
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.boss.sirofvice.HolyWrathChain
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import indigo.*
import indigo.shared.scenegraph.Layer.Content

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/** Everything Sir of Vice needs on screen:
  *   - its [[SacredGround]] aura, so that ranged players know where not to stand;
  *   - while it casts [[CleansingNova]], a wave growing from the boss, which reaches the walls of
  *     the room when the nova hits;
  *   - while it casts [[HolyWrath]], the zone around its target where other players must not stand,
  *     and then, briefly, the path followed by the wrath.
  */
object SirOfViceDrawer extends game.drawers.DrawerWithCloneBlanks {

  private val auraColour  = RGBA.fromColorInts(255, 200, 60)
  private val novaColour  = RGBA.White
  private val wrathColour = RGBA.fromColorInts(255, 240, 150)

  override def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point): Content =
    Content.empty

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.bosses.values.toJSArray.collect { case boss: SirOfVice => boss }.flatMap { boss =>
      val centre = gameToLocal(boss.currentPosition(gameState.time))

      val aura = Shape
        .Circle(
          centre,
          SacredGround.radius.toInt,
          Fill.Color(auraColour.withAlpha(0.08)),
          Stroke(2, auraColour.withAlpha(0.5))
        )
        .withDepth(Depth.far)

      val maybeNova = gameState.castingEntityInfo
        .get(boss.id)
        .filter(_.isCasting(gameState.time, 0L))
        .collect {
          case info if info.ability.isInstanceOf[CleansingNova] =>
            val progress =
              1.0 - info.remainingCastingTime(gameState.time, 0L).toDouble / info.castingTime
            Shape
              .Circle(
                centre,
                (progress * SirOfVice.roomRadius).toInt max 1,
                Fill.Color(novaColour.withAlpha(0.1)),
                Stroke(3, novaColour.withAlpha(0.6))
              )
              .withDepth(Depth.far)
        }

      val maybeWrathZone = for {
        info   <- gameState.castingEntityInfo.get(boss.id).filter(_.isCasting(gameState.time, 0L))
        wrath  <- Some(info.ability).collect { case wrath: HolyWrath => wrath }
        target <- gameState.players.get(wrath.targetId)
      } yield Shape
        .Circle(
          gameToLocal(target.currentPosition(gameState.time)),
          HolyWrath.jumpRange.toInt,
          Fill.Color(wrathColour.withAlpha(0.15)),
          Stroke(2, wrathColour.withAlpha(0.8))
        )
        .withDepth(Depth.far)

      js.Array(aura) ++ maybeNova.toJSArray ++ maybeWrathZone.toJSArray
    } ++ drawHolyWrathChains(gameState, gameToLocal)

  private def drawHolyWrathChains(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState
      .allTEntities[HolyWrathChain]
      .values
      .filter(chain => gameState.time - chain.time < HolyWrathChain.displayDuration)
      .toJSArray
      .flatMap { chain =>
        val alpha  = 1.0 - (gameState.time - chain.time).toDouble / HolyWrathChain.displayDuration
        val points = chain.positions.map(gameToLocal)
        points
          .zip(points.drop(1))
          .toJSArray
          .map((from, to) =>
            Shape.Line(from, to, Stroke(4, wrathColour.withAlpha(alpha))): SceneNode
          )
      }

}
