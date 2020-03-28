package frontend.components.forms

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import com.raquo.laminar.api.L._
import com.raquo.laminar.emitter.EventPropEmitter
import com.raquo.laminar.nodes.ReactiveElement
import errors.ErrorADT
import frontend.components.Component
import models.validators.FieldsValidator
import org.scalajs.dom
import org.scalajs.dom.html.Form
import utils.laminarzio.Implicits._
import zio.UIO

/**
  * A [[SimpleForm]] is a wrapper around functionalities of a stream of data changing through time.
  * It is called like that because it is perfectly suited to model simple forms components.
  *
  * The changing data is exposed as a [[com.raquo.airstream.signal.Signal]] (which means that it emits only when there
  * are actual changes. The [[SimpleForm#makeDataChanger]] method is a helper for creating
  * [[com.raquo.airstream.core.Observer]] changing the value of the FormData contained.
  *
  * The `submit` value is an element modifier that feeds, on submit, a stream `$submitEvents` which
  * - gathers the current form data value
  * - execute the `submitProgram`
  * - returns the result of the `submitProgram`.
  * The `submitProgram` must be an unexceptional IO, whose return type is specified in the `SubmitReturn` type param.
  *
  * The stream `$isSubmitting` allows you to disable any form that should not be submitted multiple times.
  *
  * @example
  *          A typical pattern would be:
  *          You have a case class `User(name: String, email: String)`. You then create two inputs, one for the name
  *          and one for the email. The input for the name will emit it's `onChange` (or `onInput`) event to an
  *          observer created using `makeDataChanger((name: String) => _.copy(name = name))`.
  *
  * @tparam FormData type of data contained in the form.
  * @tparam SubmitReturn type of the data that the `submitProgram` will return
  */
trait SimpleForm[FormData, SubmitReturn] { self: Component[_] =>

  implicit private def owner: Owner = self.element

  type FormDataChanger = FormData => FormData

  val initialData: FormData

  val validator: FieldsValidator[FormData, ErrorADT]

  private val dataChangers = new EventBus[FormDataChanger]()

  lazy val $formData: Signal[FormData] = dataChangers.events.fold(initialData) {
    case (data, changer) => changer(data)
  }

  lazy val $errors: EventStream[Map[String, List[ErrorADT]]] = $formData.changes.debounce(200).map(validator.validate)

  lazy val $debug: EventStream[FormData] = $formData.changes.filter(_ => scala.scalajs.LinkingInfo.developmentMode)

  def makeDataChanger[T](dataChanger: T => FormDataChanger): Observer[T] =
    dataChangers.writer.contramap(dataChanger)

  private val submitBus: EventBus[Unit] = new EventBus
  val submit: EventPropEmitter[TypedTargetEvent[Form], Unit, ReactiveElement[dom.Element]] = onSubmit.preventDefault
    .mapTo(()) --> submitBus.writer

  private lazy val $formDataView = $formData.observe

  def formDataNow: FormData = $formDataView.now

  def submitProgram(formData: FormData): UIO[SubmitReturn]

  private val isSubmittingBus: EventBus[Boolean] = new EventBus[Boolean]
  final val $isSubmitting                        = isSubmittingBus.events

  val $submitEvents: EventStream[SubmitReturn] = submitBus.events
    .mapTo(isSubmittingBus.writer.onNext(true))
    .mapTo($formDataView.now)
    .map(submitProgram)
    .flatMap(EventStream.fromZIOEffect)
    .map(traverse => {
      isSubmittingBus.writer.onNext(false)
      traverse
    })

}
