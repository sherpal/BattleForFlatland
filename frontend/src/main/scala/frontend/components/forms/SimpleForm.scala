package frontend.components.forms

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import errors.ErrorADT
import models.validators.FieldsValidator

trait SimpleForm[FormData] {

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

}
