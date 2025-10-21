package application.ai.goodais.bosses.boss101

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

final class Boss101Container extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForBoss101(index, id)
  override def square(index: Int, id: Id): SquareAIController     = SquareForBoss101(index, id)
  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForBoss101(index, id)
  override def hexagon(index: Int, id: Id): HexagonAIController   = HexagonForBoss101(index, id)
}
