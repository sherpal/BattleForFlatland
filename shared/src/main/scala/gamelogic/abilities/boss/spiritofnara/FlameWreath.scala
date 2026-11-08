package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.spiritofnara.FlameWreathRing
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.AddFlameWreath
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

import scala.util.Random

/** Puts a [[FlameWreathRing]] around [[FlameWreath.targets]] random players. Nobody may cross a
  * ring until they vanish (see [[gamelogic.buffs.boss.spiritofnara.FlameWreathWatch]]).
  */
final case class FlameWreath(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[FlameWreath.type] {
  def metadata = FlameWreath

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(FlameWreath.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val players   = gameState.players.values.toVector
    val positions = players.map(player => player.id -> player.currentPosition(time)).toMap
    val centres   = Random.shuffle(players).take(FlameWreath.targets).map(p => positions(p.id))
    val insiders = centres.map(centre =>
      positions.collect {
        case (playerId, position) if position.distanceTo(centre) < FlameWreathRing.radius =>
          playerId
      }.toVector
    )
    Vector(
      AddFlameWreath(
        genActionId(),
        time,
        genBuffId(),
        casterId,
        centres.map(_ => genEntityId()),
        centres,
        insiders
      )
    )
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): FlameWreath =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object FlameWreath extends AbilityMetadata {
  val name: String             = "Flame Wreath"
  val cooldown: Long           = MagneticPull.cooldown
  val castingTime: Long        = 2500L
  val timeToFirstAbility: Long = MagneticPull.timeToFirstAbility

  inline def targets: Int     = 2
  inline def manaCost: Double = 40.0

  def abilityId: AbilityId = Ability.spiritOfNaraFlameWreath
}
