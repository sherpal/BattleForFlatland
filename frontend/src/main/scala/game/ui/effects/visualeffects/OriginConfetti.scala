package game.ui.effects.visualeffects

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.{JSBracketAccess, JSGlobal, JSGlobalScope, JSImport, JSName}

@js.native
trait OriginConfetti extends js.Object {

  /**
    * The x position on the page, with 0 being the left edge and 1 being the right edge.
    * @default 0.5
    */
  var x: js.UndefOr[Double] = js.native

  /**
    * The y position on the page, with 0 being the left edge and 1 being the right edge.
    * @default 0.5
    */
  var y: js.UndefOr[Double] = js.native
}
object OriginConfetti {

  @scala.inline
  def apply(): OriginConfetti = {
    val __obj = js.Dynamic.literal()
    __obj.asInstanceOf[OriginConfetti]
  }

  @scala.inline
  implicit class OriginOps[Self <: OriginConfetti](val x: Self) extends AnyVal {

    @scala.inline
    def duplicate: Self = (js.Dynamic.global.Object.assign(js.Dynamic.literal(), x)).asInstanceOf[Self]

    @scala.inline
    def combineWith[Other <: js.Any](other: Other): Self with Other =
      (js.Dynamic.global.Object
        .assign(js.Dynamic.literal(), x, other.asInstanceOf[js.Any]))
        .asInstanceOf[Self with Other]

    @scala.inline
    def set(key: String, value: js.Any): Self = {
      x.asInstanceOf[js.Dynamic].updateDynamic(key)(value)
      x
    }

    @scala.inline
    def setX(value: Double): Self = this.set("x", value.asInstanceOf[js.Any])

    @scala.inline
    def deleteX: Self = this.set("x", js.undefined)

    @scala.inline
    def setY(value: Double): Self = this.set("y", value.asInstanceOf[js.Any])

    @scala.inline
    def deleteY: Self = this.set("y", js.undefined)
  }
}
