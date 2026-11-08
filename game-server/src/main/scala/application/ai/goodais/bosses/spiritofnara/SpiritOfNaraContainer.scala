package application.ai.goodais.bosses.spiritofnara

import application.ai.GoodAIManager.BossAIContainer
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import gamelogic.entities.Entity.Id

class SpiritOfNaraContainer extends BossAIContainer {
  override def triangle(index: Int, id: Id): TriangleAIController =
    TriangleForSpiritOfNara(index, id)

  override def square(index: Int, id: Id): SquareAIController = SquareForSpiritOfNara(index, id)

  override def pentagon(index: Int, id: Id): PentagonAIController =
    PentagonForSpiritOfNara(index, id)

  override def hexagon(index: Int, id: Id): HexagonAIController = HexagonForSpiritOfNara(index, id)
}
