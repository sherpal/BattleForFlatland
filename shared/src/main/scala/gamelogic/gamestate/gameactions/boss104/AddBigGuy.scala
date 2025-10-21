package gamelogic.gamestate.gameactions.boss104

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.BigGuyKick
import gamelogic.buffs.Buff
import gamelogic.buffs.ai.DamageThreatAware
import gamelogic.buffs.boss.boss104.BigGuyCurse
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.BigGuy
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.physics.Complex
import models.syntax.Pointed

final case class AddBigGuy(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    curseBuffId: Buff.Id,
    threatAwareBuffId: Buff.Id,
    position: Complex
) extends GameAction
    with EntityCreatorAction {
  override def createGameStateTransformer(gameState: GameState): GameStateTransformer = WithEntity(
    BigGuy(
      entityId,
      time,
      position,
      0.0,
      0.0,
      BigGuy.fullSpeed,
      moving = false,
      BigGuy.maxLife,
      Map.empty,
      entityId,
      Map(
        Ability.boss104BigGuyKick -> Pointed[BigGuyKick].unit.copy(
          time = time - BigGuyKick.cooldown + BigGuyKick.timeToFirstUse
        )
      )
    ),
    time
  ) ++ WithBuff(DamageThreatAware(threatAwareBuffId, entityId, entityId, time)) ++ WithBuff(
    BigGuyCurse(curseBuffId, entityId, entityId, time)
  )

  override def isLegal(gameState: GameState): Option[String] = None

  override def changeId(newId: Id): GameAction = copy(id = newId)
}
