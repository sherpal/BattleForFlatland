package game.ui.components

import game.scenes.ingame.InGameScene
import game.IndigoViewModel
import gamelogic.entities.Entity
import indigo.shared.FrameContext
import game.scenes.ingame.InGameScene.StartupData
import indigo.*
import game.ui.components.buffcontainers.BuffContainer

import scala.scalajs.js
import models.bff.outofgame.PlayerClasses
import assets.Asset
import game.events.CustomIndigoEvents
import indigo.shared.events.MouseEvent.Click
import game.ui.*
import assets.fonts.Fonts
import gamelogic.abilities.WithTargetAbility

/** @param myId
  *   id of the playing player
  * @param playerId
  *   id of the player we display the info of
  * @param playerClass
  *   class of the player we display the info of
  */
final case class PlayerFrame(
    myId: Entity.Id,
    playerId: Entity.Id,
    playerClass: PlayerClasses,
    anchor: Anchor = Anchor.topLeft
)(using context: FrameContext[StartupData], viewModel: IndigoViewModel)
    extends Component {

  val maybeAlivePlayer = viewModel.gameState.players.get(playerId)
  val maybeDeadPlayer  = viewModel.gameState.deadPlayers.get(playerId)

  def idDead = maybeDeadPlayer.isDefined

  val maybePlayer = maybeAlivePlayer.orElse(maybeDeadPlayer)

  lazy val alpha: Double =
    if myId != playerId then
      (for {
        player <- maybeAlivePlayer
        me     <- viewModel.gameState.players.get(myId)
      } yield {
        val playerPos = player.currentPosition(viewModel.gameState.time)
        val myPos     = me.currentPosition(viewModel.gameState.time)
        if (playerPos - myPos).modulus2 > WithTargetAbility.healRangeSquared then 0.5 else 1.0
      }).getOrElse(1.0)
    else 1.0

  private val theWidth  = 200
  private val theHeight = 55

  override def width: Int  = theWidth
  override def height: Int = theHeight

  override def registerEvents(bounds: Rectangle): js.Array[Component.EventRegistration[?]] =
    js.Array(
      registerClickInBounds(bounds)(js.Array(CustomIndigoEvents.GameEvent.ChooseTarget(playerId)))
    )

  override def visible: Boolean = true

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] =
    maybePlayer match {
      case None => js.Array()
      case Some(player) =>
        val playerImageAsset = Asset.playerClassAssetMap(player.cls)
        js.Array(
          Shape
            .Box(
              bounds,
              fill = Fill.Color(RGBA.fromColorInts(204, 255, 255).withAlpha(alpha)),
              stroke = Stroke(1, RGBA.Black)
            )
            .withDepth(Depth.far),
          playerImageAsset.indigoGraphic(
            bounds.position + Point(10),
            Some(RGBA.fromColorInts(player.rgb._1, player.rgb._2, player.rgb._3).withAlpha(alpha)),
            Radians.zero,
            Size(20)
          )
        )
    }

  val lifeBar = new Container(180, 20, Anchor.topRight) {

    def children: js.Array[Component] = js.Array(
      StatusBar(
        maybePlayer.fold(0.0)(_.life),
        maybePlayer.fold(1.0)(_.maxLife),
        StatusBar.lifeStatusColor,
        Asset.ingame.gui.bars.lifeBarWenakari,
        StatusBar.Horizontal,
        this.width,
        this.height,
        Anchor.right
      ),
      TextComponent(
        maybePlayer.fold("Dead")(target => s"${target.life}/${target.maxLife}"),
        Anchor.right.withOffset(Point(-2, 0)),
        this.width,
        12,
        "black",
        Fonts.xs,
        textAlign = TextAlignment.Right
      )
    )

  }

  val resourceBar = new Container(180, 15, anchor = Anchor.topRight.withOffset(Point(0, 20))) {
    def children: js.Array[Component] = js.Array(
      StatusBar(
        maybePlayer.fold(0.0)(_.resourceAmount.amount),
        maybePlayer.fold(1.0)(_.maxResourceAmount),
        _ =>
          maybePlayer.fold(RGBA.White)(player =>
            val resourceColour = player.resourceType.colour
            RGBA.fromColorInts(resourceColour.red, resourceColour.green, resourceColour.blue)
          ),
        Asset.ingame.gui.bars.minimalist,
        StatusBar.Horizontal,
        this.width,
        this.height,
        Anchor.left
      ),
      TextComponent(
        maybePlayer.fold("")(entity =>
          s"${entity.resourceAmount.amount}/${entity.maxResourceAmount}"
        ),
        Anchor.right.withOffset(Point(-2, 0)),
        this.width,
        8,
        "black",
        Fonts.xs,
        textAlign = TextAlignment.Right
      )
    )
  }

  def children: js.Array[Component] =
    js.Array(
      lifeBar,
      resourceBar,
      BuffContainer(playerId, Anchor.topLeft.withOffset(Point(0, 35))),
      TextComponent(
        maybePlayer.fold("")(_.name),
        Anchor.topLeft.withOffset(Point(20, 0)),
        width,
        height,
        "black",
        Fonts.m
      )
    )
}
