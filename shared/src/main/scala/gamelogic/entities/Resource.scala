package gamelogic.entities

import io.circe.{Decoder, Encoder}
import utils.misc.RGBColour

sealed trait Resource {
  def colour: RGBColour
}

object Resource {

  case class ResourceAmount(amount: Double, resourceType: Resource) {
    def +[R1 <: Resource](that: ResourceAmount): ResourceAmount =
      if (this.resourceType == that.resourceType) ResourceAmount(this.amount + that.amount, resourceType)
      else this

    def -[R1 <: Resource](that: ResourceAmount): ResourceAmount =
      if (this.resourceType == that.resourceType) ResourceAmount(this.amount - that.amount, resourceType)
      else this
  }

  case object Mana extends Resource {
    def colour: RGBColour = RGBColour.fromIntColour(0x0000FF)
  }
  case object Energy extends Resource {
    def colour: RGBColour = RGBColour.fromIntColour(0xFFFF00)
  }
  case object Rage extends Resource {
    def colour: RGBColour = RGBColour.fromIntColour(0xFF0000)
  }

  case object NoResource extends Resource {
    def colour: RGBColour = RGBColour.fromIntColour(0)
  }

  final val resources: Map[String, Resource] = Map(
    Mana.toString -> Mana,
    Energy.toString -> Energy,
    NoResource.toString -> NoResource
  )
  private def fromString(resourceStr: String): Resource = resources(resourceStr)

  implicit final val resourceDecoder: Decoder[Resource] = Decoder.decodeString.map(fromString)
  implicit final val resourceEncoder: Encoder[Resource] = Encoder.encodeString.contramap(_.toString)

}
