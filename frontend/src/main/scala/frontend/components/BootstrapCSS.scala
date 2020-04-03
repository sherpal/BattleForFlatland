package frontend.components

import com.raquo.laminar.api.L._

object BootstrapCSS {

  /** Buttons */
  final val btnPrimary   = className := "btn btn-primary"
  final val btnSecondary = className := "btn btn-secondary"

  /** Texts */
  final val textInfo   = className := "text-info"
  final val textDanger = className := "text-danger"
  final val textWaring = className := "text-warning"

  /** Forms */
  final val formGroup       = className := "form-group"
  final val formControl     = className := "form-control"
  final val invalidFeedback = className := "invalid-feedback"

  /** Badges */
  final val badgePill = className := "badge badge-pill"

  /** Popovers */
  final val popover                                             = dataAttr("toggle") := "popover"
  final def placement(position: String): Modifier[HtmlElement]  = dataAttr("placement") := position
  final def originalTitle(title: String): Modifier[HtmlElement] = dataAttr("original-title") := title

}
