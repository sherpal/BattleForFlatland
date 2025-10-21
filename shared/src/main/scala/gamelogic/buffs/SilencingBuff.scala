package gamelogic.buffs

import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.UseAbility

trait SilencingBuff extends Buff with ActionPreventerBuff {
  def isActionPrevented(action: GameAction): Boolean = action match {
    case action: UseAbility => action.casterId == bearerId
    case _                  => false
  }
}
