package gamelogic.entities

import io.circe.{Decoder, Encoder}
import utils.misc.RGBColour

sealed trait Resource {
  def colour: RGBColour
}

object Resource {

  case class ResourceAmount(amount: Double, resourceType: Resource) extends PartiallyOrdered[ResourceAmount] {
    def +[R1 <: Resource](that: ResourceAmount): ResourceAmount =
      if (this.resourceType == that.resourceType) ResourceAmount(this.amount + that.amount, resourceType)
      else this

    def -[R1 <: Resource](that: ResourceAmount): ResourceAmount =
      if (this.resourceType == that.resourceType) ResourceAmount(this.amount - that.amount, resourceType)
      else this

    def max(x: Double): ResourceAmount = ResourceAmount(x max amount, resourceType)
    def min(x: Double): ResourceAmount = ResourceAmount(x min amount, resourceType)

    /**
      * Returns a [[ResourceAmount]] whose amount is between 0 and `maxValue`.
      */
    def clampTo(maxValue: Double): ResourceAmount = ResourceAmount((amount max 0) min maxValue, resourceType)

    def tryCompareTo[B >: ResourceAmount](that: B)(implicit evidence$1: AsPartiallyOrdered[B]): Option[Int] =
      that match {
        case that: ResourceAmount if that.resourceType == this.resourceType => Some(this.amount compare that.amount)
        case _                                                              => None
      }
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

  val noResourceAmount: ResourceAmount = ResourceAmount(0.0, NoResource)

  final val resources: Map[String, Resource] = Map(
    Mana.toString -> Mana,
    Energy.toString -> Energy,
    NoResource.toString -> NoResource
  )
  private def fromString(resourceStr: String): Resource = resources(resourceStr)

  implicit final val resourceDecoder: Decoder[Resource] = Decoder.decodeString.map(fromString)
  implicit final val resourceEncoder: Encoder[Resource] = Encoder.encodeString.contramap(_.toString)

}
