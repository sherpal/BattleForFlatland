package game.ai.goodais.boss.goodaiscreators

import gamelogic.entities.boss.dawnoftime.Boss110
import models.bff.outofgame.gameconfig.PlayerName
import game.ai.goodais.boss.GoodAICreator
import game.ai.goodais.boss.boss110.SquareToTheLeftBoss110
import game.ai.goodais.boss.boss110.SquareToTheRightBoss110
import models.bff.outofgame.PlayerClasses
import game.ai.goodais.boss.boss110.PentagonForBoss110
import game.ai.goodais.boss.boss110.HexagonForBoss110
import game.ai.goodais.boss.boss110.TriangleForBoss110

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
    case PlayerName.AIPlayerName(PlayerClasses.Pentagon, index) =>
      GoodAICreator.defaultAICreator(new PentagonForBoss110(index)(_, _))
    case PlayerName.AIPlayerName(PlayerClasses.Hexagon, index) =>
      GoodAICreator.defaultAICreator(new HexagonForBoss110(index)(_, _))
    case PlayerName.AIPlayerName(PlayerClasses.Triangle, index) =>
      GoodAICreator.defaultAICreator(new TriangleForBoss110(index)(_, _))
  }

}
