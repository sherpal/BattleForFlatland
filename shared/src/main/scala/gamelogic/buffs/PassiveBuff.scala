package gamelogic.buffs

import gamelogic.gamestate.GameAction

/**
  * A [[gamelogic.buffs.PassiveBuff]] affects its bearer in a passive way for the duration of the buff.
  *
  * Passive buffs have the ability to add actions when they arrive and when they leave. A bubble protecting from
  * everything could for example also remove all buffs emanating from enemies.
  *
  * Examples include slow the unit down, reducing damage it receives...
  */
trait PassiveBuff extends Buff {

  /**
    * Specifies how actions are transformed during the time the buff is present.
    *
    * Action transformers must be commutative, and they should not alter the legality of actions involved. For example,
    * a buff putting the speed of a unit to 0 should not remove update in position, but should rather set the new
    * position to the previous one.
    *
    * For example, a buff can reduce damages taken during the time it is active. In that case, it would transform any
    * damage dealing action on the bearer so that the amount of damage is reduced.
    *
    * Note that the created actions don't need to have a relevant [[gamelogic.gamestate.GameAction.Id]] since they are
    * not added, removed or passed through communication protocol. They are created on the fly when the original action
    * should be created. It can thus be set as the same id as the original action.
    */
  def actionTransformer(gameAction: GameAction): List[GameAction]

}
