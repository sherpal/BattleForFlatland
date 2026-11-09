package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Check that each bomb pod touches exactly one player. If not, they explode and they kill
  * everybody (dealing them [[ExplodeBombs.damage]]). Otherwise, they do nothing.
  */
final case class ExplodeBombs(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[ExplodeBombs.type] {

  def metadata: ExplodeBombs.type = ExplodeBombs

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val bombs = gameState.entities.values.collect {
      case bomb: BombPod if bomb.powderMonkeyId == casterId => bomb
    }.toVector

    bombs
      .find(!ExplodeBombs.isCovered(gameState, _, time))
      .toVector
      .flatMap(_ =>
        gameState.players.values.map(player =>
          EntityTakesDamage(
            genActionId(),
            time,
            player.id,
            ExplodeBombs.damage,
            casterId,
            abilityId
          )
        )
      ) ++ bombs.map(bomb => RemoveEntity(genActionId(), time, bomb.id))
  }

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using i18n: I18n): Option[String] =
    gameState.entities.values.find {
      case BombPod(_, _, _, powderMonkeyId) if casterId == powderMonkeyId => true
      case _ => false
    } match {
      case Some(_) => None
      case None => Some(i18n.noBombToExplode)
    }

}

object ExplodeBombs extends AbilityMetadata {

  def name: String = "Explode bombs!"

  def cooldown: Long = 8000L

  def castingTime: Long = 1000L

  def timeToFirstAbility: Long = 0L

  def abilityId: Ability.AbilityId = Ability.boss110.explodeBombsId

  /** Enough to kill anybody. */
  inline def damage: Double = 1000.0

  /** Whether the bomb is covered by exactly one player, which is what it takes for it not to
    * explode.
    */
  def isCovered(gameState: GameState, bomb: BombPod, time: Long): Boolean =
    gameState.players.values.count(_.collides(bomb, time)) == 1

}
