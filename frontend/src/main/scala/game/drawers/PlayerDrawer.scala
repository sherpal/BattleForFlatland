package game.drawers

import assets.Asset
import game.gameutils.{toIndigo, LocalCache}
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import indigo.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object PlayerDrawer extends Drawer {

  private val colorCache = LocalCache((_: Entity.Id, player: PlayerClass) =>
    RGBA.fromColorInts(player.rgb._1, player.rgb._2, player.rgb._3)
  )

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.players.values.toJSArray.flatMap { player =>
    val playerColor      = colorCache.retrieve(player.id, player)
    val currentPlayerPos = player.currentPosition(gameState.time)
    val localCurrentPos  = gameToLocal(currentPlayerPos)
    val localNosePos =
      gameToLocal(currentPlayerPos + Complex.polar(player.shape.radius, player.rotation))
    val barPos = gameToLocal(currentPlayerPos + Complex.i * (player.shape.radius + 10))

    val playerCastingDrawings = gameState.castingEntityInfo
      .get(player.id)
      .map { info =>
        val localHandsPos = js.Array(1, -1).map { offset =>
          gameToLocal(
            currentPlayerPos + Complex.polar(
              player.shape.radius,
              player.rotation + Angle.Pi * Angle
                .fromDouble(offset.toDouble * 2) / player.shape.vertices.length
            )
          )
        }
        val colour = info.ability.abilityColour
        val sprite = playerCastingAbilityAnimatedSprite(colour)
          .graphic(gameState.time, Size(16))
          .withRef(Point(24))

        localHandsPos.map(sprite.withPosition)
      }
      .getOrElse(js.Array())

    val asset = Asset.playerClassAssetMap(player.cls)
    Graphic(
      Rectangle(asset.size),
      2,
      Material
        .ImageEffects(asset.assetName)
        .withTint(playerColor)
    ).withPosition(localCurrentPos)
      .withRef(asset.center)
      .withRotation(Radians(-player.rotation.value))
      .withScale(asset.scaleTo(2 * player.shape.radius)) +: Shape.Circle(
      localNosePos,
      2,
      Fill.Color(RGBA.White)
    ) +:
      (playerCastingDrawings ++ minilifebar(player, barPos).presentWithChildrenWithoutRectangle)
  }

  private val playerCastingAbilityAsset =
    Asset.ingame.gui.abilities.playerCastingAbilityAnimation

  private val playerCastingAbilityAnimatedSprite = Ability.abilitiesColours.map { colour =>
    colour -> LoopingAnimatedSprite(
      playerCastingAbilityAsset,
      50L,
      6,
      5,
      maybeTint = Some(colour.toIndigo)
    )
  }.toMap

}
