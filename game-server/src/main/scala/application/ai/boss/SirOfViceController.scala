package application.ai.boss

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.UseId
import gamelogic.abilities.boss.sirofvice.{
  CleansingNova,
  HolyFlame,
  HolyWrath,
  Punishment,
  SacredGround
}
import gamelogic.buffs.boss.sirofvice.Inflamed
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

/** The brain of [[SirOfVice]].
  *
  * It chases its target around the pillars of the room, and uses its abilities by decreasing order
  * of priority. Its aura, [[SacredGround]], is used each time it is available.
  */
object SirOfViceController extends AIController[SirOfVice, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: SirOfVice,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)
      .flatten
      .map { target =>
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          timeSinceLastFrame,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime + 100),
          SirOfVice.meleeRange,
          SirOfVice.fullSpeed,
          SirOfVice.fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph
        )

        def maybeCast(ability: Ability): Option[EntityStartsCasting] =
          Some(ability)
            .filter(me.canUseAbilityBoolean(_, startTime, currentGameState))
            .map(_.toStartCasting(startTime))

        // anyone but the tank, if possible someone who is not already burning
        def maybeHolyFlame = {
          val alreadyInflamed = currentGameState.allTBuffs[Inflamed].map(_.bearerId).toSet
          val candidates = currentGameState.players.values.toVector.filterNot(_.id == target.id)
          val preferred  = candidates.filterNot(player => alreadyInflamed.contains(player.id))
          Random
            .shuffle(if preferred.nonEmpty then preferred else candidates)
            .headOption
            .orElse(Some(target))
            .flatMap(victim => maybeCast(HolyFlame(UseId.dummy, startTime, me.id, victim.id)))
        }

        // anyone, tank included
        def maybeHolyWrath =
          Random
            .shuffle(currentGameState.players.values.toVector)
            .headOption
            .flatMap(victim => maybeCast(HolyWrath(UseId.dummy, startTime, me.id, victim.id)))

        useAbility(
          Vector(
            maybeCast(CleansingNova(UseId.dummy, startTime, me.id)),
            maybeCast(Punishment(UseId.dummy, startTime, me.id)),
            maybeHolyWrath,
            maybeHolyFlame,
            maybeCast(
              SacredGround(UseId.dummy, startTime, me.id, currentPosition, SacredGround.radius)
            ),
            me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Vector.empty)

  override protected def getMe(gameState: GameState, entityId: Id): Option[SirOfVice] =
    gameState.bosses.get(entityId).collect { case boss: SirOfVice => boss }

}
