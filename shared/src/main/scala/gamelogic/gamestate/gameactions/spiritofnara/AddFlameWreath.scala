package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.FlameWreathWatch
import gamelogic.entities.Entity
import gamelogic.entities.boss.spiritofnara.FlameWreathRing
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import services.i18n.I18n

/** Puts the [[FlameWreathRing]]s around the given positions, and the [[FlameWreathWatch]] on the
  * boss.
  *
  * @param insiderIds
  *   for each ring, the players inside it
  */
final case class AddFlameWreath(
    id: GameAction.Id,
    time: Long,
    watchBuffId: Buff.Id,
    bossId: Entity.Id,
    ringIds: Vector[Entity.Id],
    positions: Vector[Complex],
    insiderIds: Vector[Vector[Entity.Id]]
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    ringIds
      .lazyZip(positions)
      .lazyZip(insiderIds)
      .map((ringId, position, insiders) =>
        WithEntity(FlameWreathRing(ringId, time, position, insiders), time): GameStateTransformer
      )
      .foldLeft(GameStateTransformer.identityTransformer)(_ ++ _) ++
      WithBuff(FlameWreathWatch(watchBuffId, bossId, time, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): AddFlameWreath = copy(id = newId)
}
