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

/** Spawns a [[BigGuy]], together with the two buffs it needs to behave:
  *   - `DamageThreatAware`, without which it would never build a threat table and hence never pick
  *     a sensible target
  *   - [[gamelogic.buffs.boss.boss104.BigGuyCurse]], whose only job is to drop a
  *     [[gamelogic.entities.boss.boss104.BigGuyDeathMark]] where the add dies
  *
  * It is an [[gamelogic.gamestate.GameAction.EntityCreatorAction]], which is what lets the
  * `AIManager` (game-server) attach a controller to the freshly created entity.
  */
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
      // same "fake past usage" trick as Boss104.initialBoss: the kick becomes available
      // `timeToFirstUse` ms after the add is spawned
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
