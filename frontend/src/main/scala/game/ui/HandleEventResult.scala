// package game.ui

// import indigo.*

// import scala.scalajs.js

// final case class HandleEventResult[StartupData, ViewModel](
//     generatedEvents: js.Array[GlobalEvent],
//     continue: Boolean,
//     component: Component[StartupData, ViewModel]
// ) {
//   def stopPropagation: HandleEventResult[StartupData, ViewModel] = copy(continue = false)

//   def addGlobalEvents(events: js.Array[GlobalEvent]): HandleEventResult[StartupData, ViewModel] =
//     copy(generatedEvents = generatedEvents ++ events)

//   inline def addGlobalEvent(event: GlobalEvent): HandleEventResult[StartupData, ViewModel] =
//     addGlobalEvents(js.Array(event))
// }

// object HandleEventResult {

//   def apply[StartupData, ViewModel](
//       component: Component[StartupData, ViewModel]
//   ): HandleEventResult[StartupData, ViewModel] =
//     HandleEventResult(js.Array(), true, component)

// }
