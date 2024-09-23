package game.ui.components

import gamelogic.entities.Entity
import game.IndigoViewModel
import scala.scalajs.js
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import game.ui.*
import game.ui.Component.EventRegistration
import gamelogic.entities.{LivingEntity, MovingBody}
import gamelogic.entities.WithName
import assets.Asset
import gamelogic.entities.WithAbilities
import gamelogic.entities.Resource
import game.events.CustomIndigoEvents
import game.ui.components.buffcontainers.BuffContainer

final case class TargetFrame()(using context: FrameContext[StartupData], viewModel: IndigoViewModel)
    extends Component {

  val maybeTarget =
    viewModel.maybeTarget.flatMap(entity => viewModel.gameState.targetableEntityById(entity.id))

  val maybeTargetWithAbility = maybeTarget.collect {
    case wa: WithAbilities if wa.resourceType != Resource.NoResource => wa
  }

  override def anchor: Anchor = Anchor(
    AnchorPoint.TopLeft,
    AnchorPoint.BottomCenter,
    Point(10, -120)
  )

  override def height: Int = 55
  override def width: Int  = 200

  val lifeBar = new Container(width, 20) {

    def children: js.Array[Component] = js.Array(
      StatusBar(
        maybeTarget.fold(0.0)(_.life),
        maybeTarget.fold(1.0)(_.maxLife),
        value => if value > 0.5 then RGBA.Green else if value > 0.2 then RGBA.Orange else RGBA.Red,
        Asset.ingame.gui.bars.lifeBarWenakari,
        StatusBar.Horizontal,
        this.width,
        this.height,
        Anchor.right
      ),
      TextComponent(
        maybeTarget.fold("Dead")(target => s"${target.life}/${target.maxLife}"),
        Pixels(12),
        Anchor.right.withOffset(Point(-2, 0)),
        RGBA.Black,
        this.width,
        12,
        textAlign = TextAlign.Right
      )
    )

  }

  val resourceBar = new Container(width, 10, anchor = Anchor.topLeft.withOffset(Point(0, 20))) {
    val maybeTargetWithAbility = maybeTarget.collect {
      case wa: WithAbilities if wa.resourceType != Resource.NoResource => wa
    }
    def children: js.Array[Component] = js.Array(
      StatusBar(
        maybeTargetWithAbility.fold(0.0)(_.resourceAmount.amount),
        maybeTargetWithAbility.fold(1.0)(_.maxResourceAmount),
        _ =>
          maybeTargetWithAbility.fold(RGBA.White)(player =>
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
        maybeTargetWithAbility.fold("")(entity =>
          s"${entity.resourceAmount.amount}/${entity.maxResourceAmount}"
        ),
        Pixels(8),
        Anchor.right.withOffset(Point(-2, 0)),
        RGBA.Black,
        this.width,
        8,
        textAlign = TextAlign.Right
      )
    )
  }

  val castingBar = new Container(width, 10, Anchor.topLeft.withOffset(Point(0, 30))) {
    val maybeCastingInfo = maybeTarget.flatMap(t => viewModel.gameState.castingEntityInfo.get(t.id))
    def children: js.Array[Component] = maybeCastingInfo match {
      case None => js.Array()
      case Some(castingInfo) =>
        js.Array(
          StatusBar(
            viewModel.gameState.time - castingInfo.startedTime.toDouble,
            castingInfo.castingTime.toDouble,
            _ => RGBA.Orange,
            Asset.ingame.gui.bars.minimalist,
            StatusBar.Horizontal,
            this.width,
            this.height,
            Anchor.left
          )
        )
    }
  }

  override def children: js.Array[Component] = maybeTarget.fold[js.Array[Component]](js.Array()) {
    target =>
      val name = target match {
        case withName: WithName => withName.name
        case target             => s"Entity ${target.id}"
      }

      js.Array(
        lifeBar,
        resourceBar,
        castingBar,
        TextComponent(
          name,
          Pixels(12),
          Anchor.topLeft.withOffset(Point(1)),
          RGBA.Black,
          width,
          height
        ),
        BuffContainer(target.id, Anchor.topLeft.withOffset(Point(0, 40)))
      )
  }

  override def present(bounds: Rectangle): js.Array[SceneNode] =
    js.Array(
      Shape
        .Box(
          bounds,
          fill = Fill.Color(RGBA.fromColorInts(204, 255, 255)),
          stroke = Stroke(1, RGBA.Black)
        )
        .withDepth(Depth.far)
    )

  override def visible: Boolean = maybeTarget.isDefined

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] =
    js.Array(
      registerClickInBounds(bounds)(
        maybeTarget.fold(js.Array())(t => js.Array(CustomIndigoEvents.GameEvent.ChooseTarget(t.id)))
      )
    )

}
