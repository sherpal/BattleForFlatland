package application.ai.goodais.bosses.boss102

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class Boss102Container extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForBoss102(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForBoss102(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForBoss102(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForBoss102(index, id)
}
