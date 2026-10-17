package application.ai.goodais.bosses.sirofvice

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class SirOfViceContainer extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController = TriangleForSirOfVice(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForSirOfVice(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController = PentagonForSirOfVice(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForSirOfVice(index, id)
}
