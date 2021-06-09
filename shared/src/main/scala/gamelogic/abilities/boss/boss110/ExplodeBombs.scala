package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.RemoveEntity

/**
  * Check that each bomb pod touches exactly one player. If not, they explode and they
  * kill everybody (dealing them 1000 damage). Otherwise, they do nothing.
  */
final case class ExplodeBombs(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[ExplodeBombs.type] {

  def metadata = ExplodeBombs

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = {
    val bombs = gameState.entities.values.collect { case bomb: BombPod if bomb.powderMonkeyId == casterId => bomb }.toList

    val players = gameState.players.values

    bombs
      .find(bomb => players.count(_.collides(bomb, time)) != 1)
      .toList
      .flatMap(
        _ =>
          players.map(
            player => EntityTakesDamage(idGeneratorContainer.gameActionIdGenerator(), time, player.id, 1000, casterId)
          )
      ) ++ bombs.map(bomb => RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), time, bomb.id))
  }

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    gameState.entities.values.find {
      case BombPod(_, _, _, powderMonkeyId) if casterId == powderMonkeyId => true
      case _                                                              => false
    } match {
      case Some(_) => None
      case None    => Some("You don't own any bomb!")
    }

}

object ExplodeBombs extends AbilityMetadata {

  def name: String = "Explode bombs!"

  def cooldown: Long = 8000L

  def castingTime: Long = 1000L

  def timeToFirstAbility: Long = 0L

  def abilityId: Ability.AbilityId = Ability.boss110ExplodeBombs

}
