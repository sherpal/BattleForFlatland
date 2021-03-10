package game.ai.goodais.boss.goodaiscreators

import gamelogic.entities.boss.dawnoftime.Boss110
import models.bff.outofgame.gameconfig.PlayerName
import game.ai.goodais.boss.GoodAICreator
import game.ai.goodais.boss.boss110.SquareToTheLeftBoss110
import game.ai.goodais.boss.boss110.SquareToTheRightBoss110

/**
  * We force the metadata parameter so that we add type safety that we are using this
  * dispatcher in the right context.
  */
final class Boss110CreatorsDispatcher(val metadata: Boss110.type) extends GoodAICreatorDispatcher[Boss110.type] {

  val maybeCreatorPartial = {
    case SquareToTheLeftBoss110.name =>
      GoodAICreator.defaultAICreator(SquareToTheLeftBoss110(_, _))
    case SquareToTheRightBoss110.name =>
      GoodAICreator.defaultAICreator(SquareToTheRightBoss110(_, _))
  }

}
