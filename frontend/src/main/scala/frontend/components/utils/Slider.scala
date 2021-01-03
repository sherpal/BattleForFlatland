package frontend.components.utils

import com.raquo.laminar.api.L._

object Slider {
  def apply(
      valueStream: Signal[Double],
      updateObserver: Observer[Double],
      minValue: Double = 0.0,
      maxValue: Double = 1.0,
      step: Double     = 0.1
  ) = input(
    `type` := "range",
    className := "rounded-lg overflow-hidden appearance-none bg-gray-300 h-3 w-128",
    minAttr := minValue.toString(),
    maxAttr := maxValue.toString(),
    stepAttr := step.toString(),
    value <-- valueStream.map(_.toString),
    inContext(thisNode => onChange.mapTo(thisNode.ref.value).map(_.toDouble) --> updateObserver)
  )
}
