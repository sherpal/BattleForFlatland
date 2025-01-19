// package game.ui.components

// import game.ui.*
// import indigo.*

// import scala.scalajs.js

// case class PlainFrame[StartupData, ViewModel](
//     anchor: Anchor,
//     width: Int,
//     height: Int,
//     visible: Boolean,
//     fill: Fill,
//     stroke: Stroke
// ) extends Component[StartupData, ViewModel] {

//   def children = js.Array()

//   def withChildren(newChildren: js.Array[Component[StartupData, ViewModel]]) = this

//   def handleEvent = PartialFunction.empty

//   def present(
//       context: FrameContext[StartupData],
//       viewModel: ViewModel,
//       bounds: Rectangle
//   ): js.Array[SceneNode] = js.Array(
//     Shape.Box(
//       bounds,
//       fill,
//       stroke
//     )
//   )

// }
