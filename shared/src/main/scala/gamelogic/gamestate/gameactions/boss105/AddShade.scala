package gamelogic.gamestate.gameactions.boss105

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss105.Siphon
import gamelogic.buffs.Buff
import gamelogic.buffs.ai.DamageThreatAware
import gamelogic.buffs.boss.boss105.{ShadeBond, TwinBond}
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss105.Shade
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import models.syntax.Pointed

/** Puts the [[Shade]] in the room, with its buffs, and bonds it to the boss (see [[TwinBond]]).
  *
  * It is an [[gamelogic.gamestate.GameAction.EntityCreatorAction]], which is what lets the
  * `AIManager` (game-server) attach a controller to the freshly created entity.
  */
final case class AddShade(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    bossId: Entity.Id,
    threatAwareBuffId: Buff.Id,
    shadeBondBuffId: Buff.Id,
    twinBondBuffId: Buff.Id
) extends GameAction
    with EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(
      Shade(
        entityId,
        time,
        position,
        Angle.zero,
        Angle.zero,
        Shade.fullSpeed,
        moving = false,
        Shade.maxLife,
        Map.empty,
        entityId,
        Map(
          Ability.boss105ShadeSiphon -> Pointed[Siphon].unit.copy(
            time = time - Siphon.cooldown + Siphon.timeToFirstUse
          )
        )
      ),
      time
    ) ++
      WithBuff(DamageThreatAware(threatAwareBuffId, entityId, entityId, time)) ++
      WithBuff(ShadeBond(shadeBondBuffId, entityId, bossId, time)) ++
      WithBuff(TwinBond(twinBondBuffId, bossId, entityId, time))

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): AddShade = copy(id = newId)
}
