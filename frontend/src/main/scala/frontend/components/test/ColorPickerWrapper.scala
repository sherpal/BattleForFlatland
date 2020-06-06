package frontend.components.test

import com.raquo.airstream.core.Observer
import org.scalajs.dom.html
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import utils.misc.{Colour, RGBAColour, RGBColour}
import slinky.web.html._
import typings.reactColor.components.ChromePicker
import typings.reactColor.mod.ColorResult

@react final class ColorPickerWrapper extends Component {
  type Props = Observer[RGBAColour]
  type State = RGBAColour

  def initialState: RGBAColour = RGBColour.red

  def colorResultToRGBColour(color: ColorResult): RGBAColour =
    RGBColour(color.rgb.r.toInt, color.rgb.g.toInt, color.rgb.b.toInt).withAlpha(color.rgb.a.getOrElse(1.0))

  def render(): ReactElement = div(
    ChromePicker()
      .color(state.rgba)
      .onChangeComplete { (color, _) =>
        val newColor = colorResultToRGBColour(color)
        setState { _ =>
          props.onNext(newColor)
          newColor
        }
      }
  )
}
