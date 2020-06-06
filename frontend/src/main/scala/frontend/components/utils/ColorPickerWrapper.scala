package frontend.components.utils

import com.raquo.airstream.core.Observer
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.div
import typings.reactColor.components.ChromePicker
import typings.reactColor.mod.ColorResult
import utils.misc.{RGBAColour, RGBColour}

@react final class ColorPickerWrapper extends Component {
  case class Props(colourWriter: Observer[RGBAColour], initialColour: RGBAColour)
  type State = RGBAColour

  def initialState: RGBAColour = props.initialColour

  def colorResultToRGBColour(color: ColorResult): RGBAColour =
    RGBColour(color.rgb.r.toInt, color.rgb.g.toInt, color.rgb.b.toInt).withAlpha(color.rgb.a.getOrElse(1.0))

  def render(): ReactElement = div(
    ChromePicker()
      .color(state.rgba)
      .onChangeComplete { (color, _) =>
        val newColor = colorResultToRGBColour(color)
        setState { _ =>
          props.colourWriter.onNext(newColor)
          newColor
        }
      }
  )
}
