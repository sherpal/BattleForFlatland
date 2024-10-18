package game.ui.components.actionbar

import game.ui.{Anchor, Component}
import gamelogic.entities.classes.PlayerClass
import gamelogic.abilities.Ability
import assets.Asset
import game.ui.Anchor
import game.ui.Component.EventRegistration
import indigo.*
import scala.scalajs.js
import game.ui.components.{GraphicComponent, StatusBar}
import gamelogic.entities.EntityCastingInfo
import gamelogic.gamestate.gameactions.EntityStartsCasting

final case class AbilityIcon(
    player: PlayerClass,
    maybeEntityStartsCastingInfo: Option[EntityStartsCasting],
    abilityId: Ability.AbilityId,
    now: Long,
    iconSize: Int,
    offset: Point
) extends Component {

  override def alpha = 1.0

  val asset = Asset.abilityAssetMap(abilityId)

  override def width: Int = iconSize

  override def height: Int = iconSize

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] =
    js.Array()

  override def visible: Boolean = true

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def anchor: Anchor = Anchor.topLeft.withOffset(offset)

  def abilityJustUsedEffect(lastUsedAbilityTime: Long): js.Array[Component] = js.Array(
    GraphicComponent(
      height,
      width,
      (bounds, alpha) =>
        asset
          .indigoGraphic(
            bounds.center,
            Option.when(alpha < 1.0)(RGBA.White.withAlpha(alpha)),
            Radians.zero,
            bounds.size
          )
          .withDepth(Depth(4)),
      anchor = Anchor.topLeft.withOffset(
        if now - lastUsedAbilityTime < 100 then Point(0, 3)
        else Point.zero
      )
    )
  )

  override def children: js.Array[Component] =
    maybeEntityStartsCastingInfo.fold(abilityJustUsedEffect(0L)) { lastEntityStartsCasting =>
      abilityJustUsedEffect(lastEntityStartsCasting.time)
    } ++ (player.relevantUsedAbilities.get(abilityId) match {
      case None                                                    => js.Array()
      case Some(usedAbilityInfo) if usedAbilityInfo.cooldown == 0L => js.Array()
      case Some(usedAbilityInfo) =>
        val cooldown      = usedAbilityInfo.cooldown
        val usedSince     = now - usedAbilityInfo.time
        val remainingPerc = (cooldown - usedSince) / cooldown.toDouble
        js.Array(
          StatusBar(
            remainingPerc,
            1.0,
            _ => RGBA.Black.withAlpha(0.5),
            Asset.ingame.gui.abilities.abilityOverlay,
            StatusBar.Vertical,
            iconSize,
            iconSize,
            Anchor.topLeft
          )
        )
    })
}
