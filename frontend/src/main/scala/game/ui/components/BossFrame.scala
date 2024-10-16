package game.ui.components

import game.ui.*
import indigo.*
import scala.scalajs.js
import game.IndigoViewModel
import game.ui.Component.EventRegistration
import assets.Asset
import assets.fonts.Fonts
import game.events.CustomIndigoEvents
import game.gameutils.toIndigo

final case class BossFrame()(using viewModel: IndigoViewModel) extends Component {

  def alpha = 1.0

  val maybeBoss = viewModel.gameState.bosses.values.headOption

  override val width: Int  = 250
  override val height: Int = 30

  override inline def anchor: Anchor = Anchor.top

  override def children: js.Array[Component] = maybeBoss match {
    case None => js.Array()
    case Some(boss) =>
      val maybeCastingInfo = viewModel.gameState.castingEntityInfo.get(boss.id)

      js.Array(
        StatusBar(
          1,
          1,
          _ => RGBA.fromColorInts(156, 156, 156),
          Asset.ingame.gui.bars.lifeBarWenakari,
          StatusBar.Horizontal,
          width,
          20,
          Anchor.top
        ),
        StatusBar(
          boss.life,
          boss.maxLife,
          _ => RGBA.Green,
          Asset.ingame.gui.bars.lifeBarWenakari,
          StatusBar.Horizontal,
          width,
          20,
          Anchor.top
        ),
        TextComponent(boss.name, Anchor.top, width, 20, "black", Fonts.m)
      ) ++ maybeCastingInfo
        .map { castingInfo =>
          val colour = castingInfo.ability.abilityColour
          js.Array(
            StatusBar(
              1.0,
              1.0,
              _ => RGBA.fromColorInts(128, 128, 128),
              Asset.ingame.gui.bars.minimalist,
              StatusBar.Horizontal,
              this.width,
              10,
              Anchor.topLeft.withOffset(Point(0, 20))
            ),
            StatusBar(
              viewModel.gameState.time - castingInfo.startedTime.toDouble,
              castingInfo.castingTime.toDouble,
              _ => colour.toIndigo,
              Asset.ingame.gui.bars.minimalist,
              StatusBar.Horizontal,
              this.width,
              10,
              Anchor.topLeft.withOffset(Point(0, 20))
            ),
            TextComponent(
              boss.abilityNames(castingInfo.ability.abilityId),
              Anchor.top.withOffset(Point(0, 20)),
              this.width,
              this.height,
              if colour.isBright then "black" else "white",
              Fonts.xs
            )
          )
        }
        .getOrElse(js.Array())
  }

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def visible: Boolean = maybeBoss.isDefined

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] =
    maybeBoss.fold(js.Array())(boss =>
      js.Array(
        registerClickInBounds(bounds, stopPropagation = true)(
          js.Array(CustomIndigoEvents.GameEvent.ChooseTarget(boss.id))
        )
      )
    )

}
