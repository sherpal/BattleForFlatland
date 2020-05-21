package frontend.components.forms

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import com.raquo.laminar.api.L._
import com.raquo.laminar.modifiers.EventPropBinder
import errors.ErrorADT
import frontend.components.Component
import models.validators.FieldsValidator
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
  * Concrete implementations of the class must call the `init` method at the end of their constructor.
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

  type FormDataChanger = FormData => FormData

  /** 
   * FormData instance to use at the beginning. Typically given by an implicit [[models.syntax.Pointed]]. 
   * 
   * IMPORTANT: the initialData has to be defined at the very beginning (or even better, in the constructor) for the
   * extending concrete classes, otherwise you expose yourself to NPEs...
   */
  val initialData: FormData

  /**
    * Validator used to returns errors on FormData changes.
    * See [[models.validators.FieldsValidator]] for more details.
    */
  val validator: FieldsValidator[FormData, ErrorADT]

  private val dataChangers = new EventBus[FormDataChanger]()

  /** Signal emitting the current value of the FormData. Computed by event sourcing changes in the form. */
  lazy val $formData: Signal[FormData] = dataChangers.events.fold(initialData) {
    case (data, changer) => changer(data)
  }

  /** Emits errors in the FormData on changes. */
  lazy val $errors: EventStream[Map[String, List[ErrorADT]]] =
    $formData.changes.debounce(200).map(validator.validate)

  /** Can be used to display the FormData when on development mode. */
  lazy val $debug: EventStream[FormData] = $formData.changes.filter(_ => scala.scalajs.LinkingInfo.developmentMode)

  /**
    * Creates an observer for changing the FormData on user inputs.
    * This is typically used for each field in the form, explaining how the form will update the current data.
    *
    * @example
    *          ```
    *          case class User(name: String, age: Int)
    *
    *          val nameChanger: Observer[String] = makeDataChanger(newName => _.copy(name = newName))
    *          val ageChanger: Observer[Int] = makeDataChanger(newAge => _.copy(age = newAge))
    *
    *          input(inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger))
    *          input(
    *            inContext(
    *              elem => onChange.mapTo(Try(elem.ref.value.toInt)).collect { case Success(v) => v} --> ageChanger
    *            )
    *           )
    *          ```
    * @param dataChanger describes how the form data should be changed given an instance of T
    * @return [[com.raquo.airstream.core.Observer]] to feed from form inputs.
    */
  def makeDataChanger[T](dataChanger: T => FormDataChanger): Observer[T] =
    dataChangers.writer.contramap(dataChanger)

  private val submitBus: EventBus[Unit] = new EventBus

  /** The <form> reactive element should add this modifier to kick off submitting. */
  val submit: EventPropBinder[TypedTargetEvent[Form]] = onSubmit.preventDefault.mapTo(()) --> submitBus.writer

  /** ZIO program to be ran on submit events. */
  def submitProgram(formData: FormData): UIO[SubmitReturn]

  /**
    * Runs the submitProgram and return its value on each submit event.
    */
  lazy val $submitEvents: EventStream[SubmitReturn] = submitBus.events
    .withCurrentValueOf($formData)
    .map(_._2)
    .map(submitProgram)
    .flatMap(EventStream.fromZIOEffect)

  /** Stream indicating whether the stream is currently submitting. */
  final lazy val $isSubmitting: EventStream[Boolean] = EventStream.merge(
    submitBus.events.mapTo(true),
    $submitEvents.mapTo(false)
  )

  /**
    * Prints the data passing through the form if in dev mode.
    */
  def initDebug(owner: Owner): Unit =
    $debug.foreach(println)(owner)

}
