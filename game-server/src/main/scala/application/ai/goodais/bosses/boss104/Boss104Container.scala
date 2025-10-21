package application.ai.goodais.bosses.boss104

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class Boss104Container extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForBoss104(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForBoss104(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForBoss104(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForBoss104(index, id)
}
