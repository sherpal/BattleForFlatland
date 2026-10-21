package application.ai.goodais.bosses.boss105

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class Boss105Container extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForBoss105(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForBoss105(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForBoss105(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForBoss105(index, id)
}
