package application.ai.goodais.bosses.boss110

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class Boss110Container extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForBoss110(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForBoss110(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForBoss110(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForBoss110(index, id)
}
