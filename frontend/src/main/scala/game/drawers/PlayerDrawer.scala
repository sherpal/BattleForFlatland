package game.drawers

import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import indigo.*
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import assets.Asset
import game.ui.components.StatusBar
import game.ui.Anchor
import game.gameutils.LocalCache
import gamelogic.entities.Entity
import gamelogic.entities.classes.PlayerClass
import game.ui.Container
import game.ui.Component

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

    val asset = Asset.playerClassAssetMap(player.cls)
    Graphic(
      Rectangle(asset.size),
      2,
      Material
        .ImageEffects(asset.assetName)
        .withTint(playerColor)
    ).withPosition(localCurrentPos)
      .withRef(asset.center)
      .withRotation(Radians(-player.rotation))
      .withScale(asset.scaleTo(2 * player.shape.radius)) +: Shape.Circle(
      localNosePos,
      2,
      Fill.Color(RGBA.White)
    ) +:
      minilifebar(player, barPos).presentWithChildrenWithoutRectangle
  }

}
