package frontend.components.utils.popper
import typings.popperjsCore.enumsMod.Placement

import scala.language.implicitConversions

sealed trait PlacementOption {
  def popperPlacement: typings.popperjsCore.enumsMod.Placement
}

object PlacementOption {

  val auto: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.auto
  }
  val autoEnd: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`auto-end`
  }
  val autoStart: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`auto-start`
  }
  val bottom: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.bottom
  }
  val bottomStart: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`bottom-start`
  }
  val bottomEnd: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`bottom-end`
  }
  val left: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.left
  }
  val leftStart: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`left-start`
  }
  val leftEnd: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`left-end`
  }
  val top: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.top
  }
  val topStart: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`top-start`
  }
  val topEnd: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`bottom-end`
  }
  val right: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.right
  }
  val rightStart: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`right-start`
  }
  val rightEnd: PlacementOption = new PlacementOption {
    def popperPlacement: Placement = Placement.`right-end`
  }

  implicit def asPopperPlacement(placementOption: PlacementOption): Placement = placementOption.popperPlacement
}
