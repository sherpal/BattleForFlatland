package game.ai.goodais.boss.goodaiscreators

import gamelogic.entities.boss.Boss101
import game.ai.goodais.boss.GoodAICreator
import models.bff.outofgame.gameconfig.PlayerName
import game.ai.goodais.boss.boss101.SquareForBoss101
import models.bff.outofgame.PlayerClasses
import game.ai.goodais.boss.boss101.HealForBoss101
import game.ai.goodais.boss.boss101.PentagonForBoss101
import game.ai.goodais.boss.boss101.TriangleForBoss101

final class Boss101CreatorsDispatcher(val metadata: Boss101.type) extends GoodAICreatorDispatcher[Boss101.type] {

  val maybeCreatorPartial: PartialFunction[PlayerName.AIPlayerName, GoodAICreator[Boss101.type]] = {
    case SquareForBoss101.name =>
      GoodAICreator.defaultAICreator(SquareForBoss101(_, _))
    case PlayerName.AIPlayerName(PlayerClasses.Hexagon, index) =>
      GoodAICreator.defaultAICreator(new HealForBoss101(index)(_, _))
    case PlayerName.AIPlayerName(PlayerClasses.Pentagon, index) =>
      GoodAICreator.defaultAICreator(new PentagonForBoss101(index)(_, _))
    case PlayerName.AIPlayerName(PlayerClasses.Triangle, index) =>
      GoodAICreator.defaultAICreator(new TriangleForBoss101(index)(_, _))
  }

}
