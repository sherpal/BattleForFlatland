// package game.ui

// import indigo.*
// import scala.scalajs.js

// case class TextComponent[StartupData, ViewModel](
//     text: (FrameContext[StartupData], ViewModel) => String,
//     anchor: Anchor,
//     width: Int,
//     height: Int,
//     visible: Boolean
// ) extends Component[StartupData, ViewModel] {

//   def children: js.Array[Component[StartupData, ViewModel]] = js.Array() // No children

//   def withChildren(
//       newChildren: js.Array[Component[StartupData, ViewModel]]
//   ): Component[StartupData, ViewModel] = this

//   def handleEvent: PartialFunction[
//     (FrameContext[StartupData], ViewModel, GlobalEvent, Rectangle),
//     HandleEventResult[
//       StartupData,
//       ViewModel
//     ]
//   ] =
//     PartialFunction.empty

//   def present(
//       context: FrameContext[StartupData],
//       viewModel: ViewModel,
//       bounds: Rectangle
//   ): scala.scalajs.js.Array[SceneNode] = js.Array(
//     TextBox(text(context, viewModel), width, height)
//       .withFontFamily(FontFamily.cursive)
//       .withColor(RGBA.White)
//       .withFontSize(Pixels(16))
//       .withStroke(TextStroke(RGBA.Red, Pixels(1)))
//       .withPosition(bounds.position)
//   )

// }
