package gamelogic.buffs

import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState

/**
  * [[Buff]] can extend this trait to mark they are going to prevent some actions from
  * happenning.
  *
  * This trait is mostly useless for players, and even for the game logic in general.
  * However, it will be usefull for optimizing performances when coding AIs. Indeed, imagine
  * that an action prevents from using an ability. An AI will keep trying using the ability
  * (usually at 30FPS) which will create a lot of actions for nothing.
  */
trait ActionPreventerBuff {

  /**
    * Function determining whether this action will be prevented from happenning.
    */
  def isActionPrevented(action: GameAction): Boolean
}
