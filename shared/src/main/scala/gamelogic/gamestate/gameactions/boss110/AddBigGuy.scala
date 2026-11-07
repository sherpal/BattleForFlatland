package gamelogic.gamestate.gameactions.boss110

import gamelogic.buffs.Buff
import gamelogic.buffs.ai.DamageThreatAware
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.physics.Complex
import services.i18n.I18n

/** Spawns a [[BigGuy]], with the `DamageThreatAware` buff without which it would never build a
  * threat table (and hence never pick a sensible target).
  *
  * It is an [[gamelogic.gamestate.GameAction.EntityCreatorAction]], which is what lets the
  * `AIManager` (game-server) attach a controller to the freshly created entity.
  */
final case class AddBigGuy(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    threatAwareBuffId: Buff.Id,
    position: Complex
) extends GameAction
    with GameAction.EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(
      BigGuy(
        entityId,
        time,
        position,
        Angle.zero,
        Angle.zero,
        BigGuy.fullSpeed,
        moving = false,
        BigGuy.maxLife,
        Map.empty,
        entityId,
        Map.empty
      ),
      time
    ) ++ WithBuff(DamageThreatAware(threatAwareBuffId, entityId, entityId, time))

  def isLegal(gameState: GameState)(using I18n): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
