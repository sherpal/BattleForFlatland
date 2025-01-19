// package game.ui.components

// import game.ui.Component
// import indigo.shared.datatypes.Rectangle
// import game.ui.Anchor
// import indigo.GlobalEvent
// import game.ui.HandleEventResult
// import indigo.*

// import scala.scalajs.js

// case class Counter[StartupData, ViewModel](
//     lastUpdateTime: Seconds,
//     currentValue: Int,
//     position: Rectangle
// ) extends Component[StartupData, ViewModel] {

//   def children: js.Array[Component[StartupData, ViewModel]] = js.Array()

//   def withChildren(
//       newChildren: scala.scalajs.js.Array[Component[StartupData, ViewModel]]
//   ): Component[StartupData, ViewModel] = this

//   def anchor: Anchor = Anchor.topLeft.withOffset(position.position)

//   def width: Int  = position.width
//   def height: Int = position.height

//   def visible: Boolean = true

//   def handleEvent: PartialFunction[
//     (FrameContext[StartupData], ViewModel, GlobalEvent, Rectangle),
//     HandleEventResult[
//       StartupData,
//       ViewModel
//     ]
//   ] = {
//     case (context, model, FrameTick, rectangle)
//         if context.gameTime.running > lastUpdateTime + Seconds(1) =>
//       HandleEventResult(
//         copy(lastUpdateTime = context.gameTime.running, currentValue = currentValue + 1)
//       )
//   }

//   def present(
//       context: FrameContext[StartupData],
//       viewModel: ViewModel,
//       bounds: Rectangle
//   ): js.Array[SceneNode] = js.Array(
//     TextBox(s"Counter: $currentValue", width, height)
//       .withFontFamily(FontFamily.cursive)
//       .withColor(RGBA.White)
//       .withFontSize(Pixels(16))
//       .withStroke(TextStroke(RGBA.Red, Pixels(1)))
//       .withPosition(bounds.position)
//   )

// }
